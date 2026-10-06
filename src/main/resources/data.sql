-- Datos iniciales del sistema Biblioteca
-- Estrategia: Spring Boot ejecuta data.sql automáticamente al iniciar (con ddl-auto=update).
-- Para evitar duplicados, usamos INSERT IGNORE (MySQL) y comprobaciones WHERE NOT EXISTS.
-- Las tablas se crean/actualizan desde las entidades JPA (ddl-auto=update).

-- Usuario administrador inicial
-- Password: 'admin123' hasheado con BCrypt (cost 10)
-- Se usa INSERT IGNORE para no fallar si el email ya existe (índice único).
INSERT IGNORE INTO usuarios (email, password, estado, rol)
VALUES ('admin@biblioteca.com', '$2a$10$8K1p/a0dURXAm7QiTRqUauQbeBGVqPKhE7n5kFQXhYQOu9vXhXh2W', 'ACTIVO', 'ADMIN');

-- Libros de prueba
INSERT IGNORE INTO libros (titulo, autor, categoria, stock_total, stock_disponible, activo)
VALUES
    ('Cien años de soledad', 'Gabriel García Márquez', 'Novela', 5, 5, true),
    ('Don Quijote de la Mancha', 'Miguel de Cervantes', 'Novela', 3, 3, true),
    ('El principito', 'Antoine de Saint-Exupéry', 'Infantil', 10, 10, true),
    ('1984', 'George Orwell', 'Ciencia Ficción', 4, 4, true),
    ('Fahrenheit 451', 'Ray Bradbury', 'Ciencia Ficción', 3, 3, true),
    ('Crónica de una muerte anunciada', 'Gabriel García Márquez', 'Novela', 2, 2, true),
    ('El señor de los anillos', 'J.R.R. Tolkien', 'Fantasía', 6, 6, true),
    ('Harry Potter y la piedra filosofal', 'J.K. Rowling', 'Fantasía', 8, 8, true);