-- Datos iniciales del sistema Biblioteca.
-- Se ejecuta en cada arranque (despues de que Hibernate crea las tablas) y es idempotente:
-- usuarios: INSERT IGNORE + indice unico en email.
-- libros: INSERT ... WHERE NOT EXISTS por titulo y autor.
--
-- Usuarios de prueba (password en texto plano solo aqui, como referencia; en la BD estan hasheadas con BCrypt):
--   admin@biblioteca.com      / admin123   (ADMIN)
--   biblio@biblioteca.com     / biblio123  (BIBLIOTECARIO)
--   lector@biblioteca.com     / lector123  (LECTOR)

INSERT IGNORE INTO usuarios (email, password, estado, rol) VALUES
    ('admin@biblioteca.com', '$2a$10$4DCUQ3JHekNVNNaGGIl0iO05GDZM.ED3aHgoQhCCHK0S0pttEWawK', 'ACTIVO', 'ADMIN'),
    ('biblio@biblioteca.com', '$2a$10$2JHpEbcYWpMzZPAgJv7hleTOnJUEIh9YKlc1v1qcm87/7d0O3oNGC', 'ACTIVO', 'BIBLIOTECARIO'),
    ('lector@biblioteca.com', '$2a$10$F3FwRDwi6jpGx/E0D3o7wOJ6i1ac3ldSkqtqBDnV6l3w50ADqULWS', 'ACTIVO', 'LECTOR');

INSERT INTO libros (titulo, autor, categoria, stock_total, stock_disponible, activo)
SELECT t.titulo, t.autor, t.categoria, t.stock, t.stock, true
FROM (
    SELECT 'Cien años de soledad' AS titulo, 'Gabriel García Márquez' AS autor, 'Novela' AS categoria, 5 AS stock
    UNION ALL SELECT 'Don Quijote de la Mancha', 'Miguel de Cervantes', 'Novela', 3
    UNION ALL SELECT 'El principito', 'Antoine de Saint-Exupéry', 'Infantil', 10
    UNION ALL SELECT '1984', 'George Orwell', 'Ciencia Ficción', 4
    UNION ALL SELECT 'Fahrenheit 451', 'Ray Bradbury', 'Ciencia Ficción', 3
    UNION ALL SELECT 'Crónica de una muerte anunciada', 'Gabriel García Márquez', 'Novela', 2
    UNION ALL SELECT 'El señor de los anillos', 'J.R.R. Tolkien', 'Fantasía', 6
    UNION ALL SELECT 'Harry Potter y la piedra filosofal', 'J.K. Rowling', 'Fantasía', 8
) t
WHERE NOT EXISTS (
    SELECT 1 FROM libros l WHERE l.titulo = t.titulo AND l.autor = t.autor
);
