import {
    createContext,
    useContext,
    useEffect,
    useState,
} from "react";

import {
    authApi,
    notificationUrl,
    usersApi,
} from "./api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [token, setToken] = useState(
        localStorage.getItem("bookngo_token")
    );

    const [role, setRole] = useState(
        localStorage.getItem("bookngo_role")
    );

    const [user, setUser] = useState(null);

    const [loading, setLoading] = useState(
        Boolean(localStorage.getItem("bookngo_token"))
    );

    const [notifications, setNotifications] = useState([]);

    /* --------------------------------------------------
       LOAD CURRENT USER
    -------------------------------------------------- */

    const refreshUser = async () => {
        if (!token) {
            return null;
        }

        const response = await usersApi.getMe();

        setUser(response.data);

        return response.data;
    };

    /* --------------------------------------------------
       INITIAL USER LOAD
    -------------------------------------------------- */

    useEffect(() => {
        if (!token) {
            setLoading(false);
            return;
        }

        refreshUser()
            .catch(() => {
                setUser(null);
            })
            .finally(() => {
                setLoading(false);
            });
    }, [token]);

    /* --------------------------------------------------
       SERVER SENT EVENTS
    -------------------------------------------------- */

    useEffect(() => {
        if (!token) {
            return;
        }

        const controller = new AbortController();

        const connect = async () => {
            try {
                const response = await fetch(
                    notificationUrl(),
                    {
                        method: "GET",

                        headers: {
                            Authorization: `Bearer ${token}`,
                            Accept: "text/event-stream",
                        },

                        signal: controller.signal,
                    }
                );

                if (!response.ok || !response.body) {
                    return;
                }

                const reader =
                    response.body.getReader();

                const decoder = new TextDecoder();

                let buffer = "";

                while (true) {
                    const {
                        value,
                        done,
                    } = await reader.read();

                    if (done) {
                        break;
                    }

                    buffer += decoder.decode(
                        value,
                        {
                            stream: true,
                        }
                    );

                    const events =
                        buffer.split("\n\n");

                    buffer =
                        events.pop() || "";

                    for (const event of events) {
                        const lines =
                            event.split("\n");

                        const dataLine =
                            lines.find((line) =>
                                line.startsWith("data:")
                            );

                        if (!dataLine) {
                            continue;
                        }

                        const message =
                            dataLine
                                .substring(5)
                                .trim();

                        if (!message) {
                            continue;
                        }

                        setNotifications(
                            (previous) => [
                                {
                                    id:
                                        Date.now() +
                                        Math.random(),

                                    message,
                                },

                                ...previous,
                            ].slice(0, 10)
                        );
                    }
                }
            } catch (error) {
                if (
                    error.name !==
                    "AbortError"
                ) {
                    console.log(
                        "SSE connection ended."
                    );
                }
            }
        };

        connect();

        return () => {
            controller.abort();
        };
    }, [token]);

    /* --------------------------------------------------
       LOGIN
    -------------------------------------------------- */

    const login = async (credentials) => {
        const response =
            await authApi.login(
                credentials
            );

        const newToken =
            response.data.token;

        const newRole =
            response.data.role;

        localStorage.setItem(
            "bookngo_token",
            newToken
        );

        localStorage.setItem(
            "bookngo_role",
            newRole
        );

        setToken(newToken);
        setRole(newRole);

        return response.data;
    };

    /* --------------------------------------------------
       LOGOUT
    -------------------------------------------------- */

    const logout = () => {
        localStorage.removeItem(
            "bookngo_token"
        );

        localStorage.removeItem(
            "bookngo_role"
        );

        setToken(null);
        setRole(null);
        setUser(null);
        setNotifications([]);
    };

    return (
        <AuthContext.Provider
            value={{
                token,
                role,
                user,
                setUser,
                loading,
                login,
                logout,
                refreshUser,
                notifications,
                setNotifications,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}