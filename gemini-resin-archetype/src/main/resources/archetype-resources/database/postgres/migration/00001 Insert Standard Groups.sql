/* Inserts the standard Pyxis User Groups. */

INSERT INTO public.group (id, type, name, description) VALUES (0, 0, 'Guests', 'Guests to the system');
INSERT INTO public.group (id, type, name, description) VALUES (1, 0, 'Users', 'Standard system users');
INSERT INTO public.group (id, type, name, description) VALUES (1000, 0, 'Administrators', 'System administrators');
