INSERT INTO categories (category_name, description, active) VALUES
 ('Outdoor Adventures',  'Kayaking, camping, and outdoor experiences.', TRUE),
 ('Creative Workshops',  'Pottery, painting, and creative classes.', TRUE),
 ('Fitness & Wellness',  'Yoga, fitness, and wellness activities.', TRUE),
 ('Food & Cooking',      'Cooking classes and culinary experiences.', TRUE)
ON CONFLICT (category_name) DO NOTHING;
