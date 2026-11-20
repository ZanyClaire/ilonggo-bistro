-- Seed Store Profile for Ilonggo Bistro (Barotac Nuevo, Iloilo)
INSERT INTO store_settings (setting_key, setting_value) VALUES
('bistro_name', 'Ilonggo Bistro'),
('tagline', 'Warm Ilonggo Comfort, From First Coffee to Last Call'),
('address_line', 'Burgos-Osmeña St, Barotac Nuevo, 5007 Iloilo'),
('plus_code', 'VPW2+QF Barotac Nuevo, Iloilo'),
('google_maps_url', 'https://www.google.com/maps/search/?api=1&query=VPW2%2BQF+Barotac+Nuevo%2C+Iloilo'),
('open_time', '07:00'),
('close_time', '21:00'),
('phone_number', '+63 917 000 0000'),
('email', 'hello@ilonggobistro.ph'),
('facebook_url', 'https://facebook.com'),
('messenger_url', 'https://m.me'),
('announcement_banner', 'Serving Barotac Nuevo daily from 7:00 AM to 9:00 PM — Breakfast, Ilonggo Classics, Merienda & Dinner.'),
('max_party_size', '20'),
('hourly_seat_capacity', '40');

-- Menu Categories covering 7:00 AM to 9:00 PM dayparts
INSERT INTO menu_categories (name, slug, daypart, description, display_order) VALUES
('Breakfast & Kapehan', 'breakfast-kapehan', '7:00 AM – 11:00 AM', 'Hearty silog plates and freshly brewed Iloilo native coffee to start your morning.', 1),
('Ilonggo Heritage Classics', 'ilonggo-classics', 'All Day', 'Slow-simmered broths and heirloom recipes celebrated across Iloilo.', 2),
('Chargrill & Rice Plates', 'chargrill-rice', 'Lunch & Dinner', 'Smoky annatto-basted grill favorites served with warm garlic rice and atchara.', 3),
('Merienda & Sharing', 'merienda-sharing', '2:00 PM – 6:00 PM', 'Crisp afternoon bites and comforting bowls built for barkada catch-ups.', 4),
('Coolers, Coffee & Tablea', 'drinks-coffee', 'All Day', 'Local Iloilo tablea cacao, mountain barako brews, and fresh calamansi coolers.', 5);

-- Curated Sample Ilonggo Bistro Menu (editable via /admin/menu)
INSERT INTO menu_items (category_id, name, description, price_php, image_url, badge_label, is_featured, is_available, display_order) VALUES
((SELECT id FROM menu_categories WHERE slug = 'ilonggo-classics'),
 'Signature La Paz Batchoy',
 'Rich 8-hour pork-and-beef bone broth, fresh egg miki noodles, braised pork slices, crushed chicharon, toasted garlic, scallions, and warm puto on the side.',
 195.00, '/images/dish-batchoy.jpg', 'Bestseller', TRUE, TRUE, 1),

((SELECT id FROM menu_categories WHERE slug = 'chargrill-rice'),
 'Ilonggo Chicken Inasal Pecho',
 'Charcoal-grilled chicken breast marinated in lemongrass, calamansi, garlic, and ginger, brushed with golden atsuete oil. Served on banana leaf with garlic rice and atchara.',
 225.00, '/images/dish-inasal.jpg', 'Chef''s Pick', TRUE, TRUE, 2),

((SELECT id FROM menu_categories WHERE slug = 'ilonggo-classics'),
 'Heritage KBL in Clay Palayok',
 'Tender pork hock slow-cooked with black kadyos pigeon peas, young jackfruit, and tangy batwan fruit in an earthenware pot. Good for 2–3 guests.',
 380.00, '/images/dish-kbl.jpg', 'House Signature', TRUE, TRUE, 3),

((SELECT id FROM menu_categories WHERE slug = 'ilonggo-classics'),
 'Grandma''s Pancit Molo',
 'Hand-folded pork and shrimp dumplings simmered in clear roasted-garlic chicken consommé, finished with shredded chicken and crisp fried shallots.',
 185.00, '/images/dish-pancit-molo.jpg', 'Comfort Bowl', TRUE, TRUE, 4),

((SELECT id FROM menu_categories WHERE slug = 'breakfast-kapehan'),
 'Barotac Beef Tapsilog',
 'Sweet-savory marinated sirloin strips seared crisp, paired with garlicky sinangag rice, golden sunny-side-up egg, and house spiced vinegar.',
 215.00, '/images/dish-coffee-breakfast.jpg', 'Morning Favorite', TRUE, TRUE, 5),

((SELECT id FROM menu_categories WHERE slug = 'breakfast-kapehan'),
 'Crispy Boneless Bangus Silog',
 'Marinated Iloilo milkfish belly fried golden brown with garlic rice, farm egg, and fresh tomato-ensalada.',
 220.00, '/images/dish-coffee-breakfast.jpg', 'Local Catch', FALSE, TRUE, 6),

((SELECT id FROM menu_categories WHERE slug = 'chargrill-rice'),
 'Sizzling Pork Sisig Platter',
 'Crisp chopped pork belly and cheek tossed with red onions, calamansi, and bird''s eye chili on a cast-iron skillet.',
 265.00, '/images/dish-inasal.jpg', 'For Sharing', FALSE, TRUE, 7),

((SELECT id FROM menu_categories WHERE slug = 'chargrill-rice'),
 'Inasal Pork BBQ Skewers (3 pcs)',
 'Flame-grilled pork skewers glazed with sweet-smoky Ilonggo marinade, served with steamed rice and spiced sinamak.',
 195.00, '/images/dish-inasal.jpg', NULL, FALSE, TRUE, 8),

((SELECT id FROM menu_categories WHERE slug = 'merienda-sharing'),
 'Caramelized Turon & Warm Tablea Dip',
 'Crisp saba banana and jackfruit spring rolls dusted with toasted sesame, served alongside thick melted Iloilo cacao tablea.',
 145.00, '/images/dish-merienda-drinks.jpg', 'Merienda Hit', TRUE, TRUE, 9),

((SELECT id FROM menu_categories WHERE slug = 'merienda-sharing'),
 'Toasted Ilonggo Biscocho & Kesong Puti',
 'Crisp butter-brushed toasted bread slices served warm with whipped local soft cheese and wild honey drizzle.',
 135.00, '/images/dish-merienda-drinks.jpg', NULL, FALSE, TRUE, 10),

((SELECT id FROM menu_categories WHERE slug = 'drinks-coffee'),
 'Iced Iloilo Tablea Mocha',
 'Double-shot native espresso layered over stone-ground Iloilo cacao tablea and cold velvety milk.',
 140.00, '/images/dish-coffee-breakfast.jpg', 'House Brew', TRUE, TRUE, 11),

((SELECT id FROM menu_categories WHERE slug = 'drinks-coffee'),
 'Barotac Calamansi Honey Cooler',
 'Fresh-pressed green calamansi shaken with local wildflower honey, mint leaves, and chilled sparkling water.',
 110.00, '/images/dish-merienda-drinks.jpg', 'Refreshing', FALSE, TRUE, 12),

((SELECT id FROM menu_categories WHERE slug = 'drinks-coffee'),
 'Hot Native Tsokolate Batirol',
 'Traditional whisked Iloilo cacao tablea served steaming hot with subtle muscovado sweetness.',
 125.00, '/images/dish-coffee-breakfast.jpg', 'Traditional', FALSE, TRUE, 13);

-- Seed Gallery Photos (Food & Drink, Vibe, Events)
INSERT INTO gallery_photos (title, caption, category, image_url, display_order, is_visible) VALUES
('Morning Sunlight at Burgos-Osmeña St', 'Our sunlit dining hall opens at 7:00 AM daily for coffee, breakfast, and quiet mornings.', 'Vibe', '/images/hero-bistro.jpg', 1, TRUE),
('Signature La Paz Batchoy', 'Slow-simmered broth topped with fresh chicharon, garlic oil, and served with warm puto.', 'Food & Drink', '/images/dish-batchoy.jpg', 2, TRUE),
('Golden Evening Ambiance', 'Warm rattan lighting and relaxed dinner hospitality until 9:00 PM.', 'Vibe', '/images/vibe-evening.jpg', 3, TRUE),
('Chargrilled Chicken Inasal', 'Annatto-glazed chicken grilled over live charcoal and plated on fresh banana leaf.', 'Food & Drink', '/images/dish-inasal.jpg', 4, TRUE),
('Clay-Pot KBL for the Table', 'Heirloom Kadyos, Baboy, at Langka cooked slow for family lunches and Sunday gatherings.', 'Food & Drink', '/images/dish-kbl.jpg', 5, TRUE),
('Hand-Folded Pancit Molo', 'Clear savory chicken broth brimming with delicate pork-shrimp dumplings.', 'Food & Drink', '/images/dish-pancit-molo.jpg', 6, TRUE),
('7 AM Silog & Tablea Mocha', 'Start your day in Barotac Nuevo with hearty breakfast plates and freshly pulled coffee.', 'Food & Drink', '/images/dish-coffee-breakfast.jpg', 7, TRUE),
('Afternoon Merienda & Coolers', 'Crisp turon with melted tablea dip and fresh calamansi honey coolers for barkada afternoons.', 'Events', '/images/dish-merienda-drinks.jpg', 8, TRUE);
