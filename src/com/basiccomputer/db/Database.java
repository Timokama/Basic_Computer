package com.basiccomputer.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mindrot.jbcrypt.BCrypt;

public class Database {
    private static final String URL = "jdbc:postgresql://localhost:5432/basiccomputer";
    private static final String USER = "postgres";
    private static final String PASSWORD = "secret123";
    
    private static Connection connection;
    
    public static void initialize() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found");
            throw new RuntimeException(e);
        }
        
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            createTables();
            createDefaultAdmin();
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            throw e;
        }
    }
    
    private static void createDefaultAdmin() throws SQLException {
        // Check if admin user already exists
        String checkSql = "SELECT id FROM users WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(checkSql)) {
            pstmt.setString(1, "timo.munyiri@gmail.com");
            ResultSet rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                // Admin doesn't exist, create it with bcrypt hashed password
                String hashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt());
                String insertSql = "INSERT INTO users (email, password, full_name, phone_number, role, active) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, "timo.munyiri@gmail.com");
                    insertStmt.setString(2, hashedPassword);
                    insertStmt.setString(3, "Admin User");
                    insertStmt.setString(4, "0790722419");
                    insertStmt.setString(5, "ADMIN");
                    insertStmt.setBoolean(6, true);
                    insertStmt.executeUpdate();
                    System.out.println("Default admin user created: timo.munyiri@gmail.com");
                }
            } else {
                System.out.println("Admin user already exists");
            }
        }
    }
    
    private static void createTables() throws SQLException {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                email VARCHAR(255) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                full_name VARCHAR(255) NOT NULL,
                phone_number VARCHAR(20),
                role VARCHAR(20) DEFAULT 'STUDENT',
                active BOOLEAN DEFAULT true,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createSubscriptionsTable = """
            CREATE TABLE IF NOT EXISTS subscriptions (
                id SERIAL PRIMARY KEY,
                user_id INTEGER REFERENCES users(id),
                type VARCHAR(20) NOT NULL,
                status VARCHAR(20) DEFAULT 'PENDING',
                start_date TIMESTAMP,
                end_date TIMESTAMP,
                mpesa_receipt_number VARCHAR(100),
                mpesa_transaction_id VARCHAR(100),
                amount DECIMAL(10, 2),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createContentTable = """
            CREATE TABLE IF NOT EXISTS content (
                id SERIAL PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                description TEXT,
                type VARCHAR(20) NOT NULL,
                content_url TEXT,
                body TEXT,
                category VARCHAR(50),
                created_by INTEGER REFERENCES users(id),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createProjectsTable = """
            CREATE TABLE IF NOT EXISTS projects (
                id SERIAL PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                description TEXT,
                project_url TEXT,
                status VARCHAR(20) DEFAULT 'In Progress',
                progress INTEGER DEFAULT 0,
                technologies TEXT,
                cover_color VARCHAR(255) DEFAULT 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                icon VARCHAR(10) DEFAULT '\uD83D\uDCBB',
                created_by INTEGER REFERENCES users(id),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createSubscriptionsTable);
            stmt.execute(createContentTable);
            stmt.execute(createProjectsTable);
            System.out.println("Database tables created successfully");
        }
    }
    
    public static Connection getConnection() {
        return connection;
    }
    
    // User operations
    public static boolean userExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }
    
    public static void registerUser(String email, String password, String fullName, String phone) throws SQLException {
        registerUser(email, password, fullName, phone, "STUDENT");
    }
    
    public static void registerUser(String email, String password, String fullName, String phone, String role) throws SQLException {
        // Hash the password with bcrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (email, password, full_name, phone_number, role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, fullName);
            pstmt.setString(4, phone);
            pstmt.setString(5, role);
            pstmt.executeUpdate();
        }
    }
    
    public static Map<String, Object> authenticateUser(String email, String password) throws SQLException {
        // First get user by email only
        String sql = "SELECT id, email, password, full_name, role, active, phone_number FROM users WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                boolean passwordValid = false;
                
                // Check if password is bcrypt hash or plain text
                if (storedPassword != null && (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$"))) {
                    // Use bcrypt verification
                    passwordValid = BCrypt.checkpw(password, storedPassword);
                } else {
                    // Legacy plain text password comparison
                    passwordValid = password.equals(storedPassword);
                }
                
                if (passwordValid) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getInt("id"));
                    user.put("email", rs.getString("email"));
                    user.put("full_name", rs.getString("full_name"));
                    user.put("role", rs.getString("role"));
                    user.put("active", rs.getBoolean("active"));
                    user.put("phone_number", rs.getString("phone_number"));
                    return user;
                }
            }
        }
        return null;
    }
    
    public static Map<String, Object> getUserById(int userId) throws SQLException {
        String sql = "SELECT id, email, full_name, role, active, phone_number, created_at FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("id"));
                user.put("email", rs.getString("email"));
                user.put("full_name", rs.getString("full_name"));
                user.put("role", rs.getString("role"));
                user.put("active", rs.getBoolean("active"));
                user.put("phone_number", rs.getString("phone_number"));
                user.put("created_at", rs.getTimestamp("created_at"));
                return user;
            }
        }
        return null;
    }
    
    public static Map<String, Object> getUserByEmail(String email) throws SQLException {
        String sql = "SELECT id, email, full_name, role, active, phone_number, created_at FROM users WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("id"));
                user.put("email", rs.getString("email"));
                user.put("full_name", rs.getString("full_name"));
                user.put("role", rs.getString("role"));
                user.put("active", rs.getBoolean("active"));
                user.put("phone_number", rs.getString("phone_number"));
                user.put("created_at", rs.getTimestamp("created_at"));
                return user;
            }
        }
        return null;
    }
    
    public static List<Map<String, Object>> getAllUsers() throws SQLException {
        List<Map<String, Object>> users = new ArrayList<>();
        String sql = "SELECT id, email, full_name, role, active, phone_number, created_at FROM users ORDER BY created_at DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("id"));
                user.put("email", rs.getString("email"));
                user.put("full_name", rs.getString("full_name"));
                user.put("role", rs.getString("role"));
                user.put("active", rs.getBoolean("active"));
                user.put("phone_number", rs.getString("phone_number"));
                user.put("created_at", rs.getTimestamp("created_at"));
                users.add(user);
            }
        }
        return users;
    }
    
    public static List<Map<String, Object>> getAllContent() throws SQLException {
        List<Map<String, Object>> contentList = new ArrayList<>();
        String sql = "SELECT id, title, type, category, description, body, content_url, created_at FROM content ORDER BY created_at DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> content = new HashMap<>();
                content.put("id", rs.getInt("id"));
                content.put("title", rs.getString("title"));
                content.put("content_type", rs.getString("type"));
                content.put("category", rs.getString("category"));
                content.put("description", rs.getString("description"));
                content.put("body", rs.getString("body"));
                content.put("created_at", rs.getTimestamp("created_at"));
                contentList.add(content);
            }
        }
        return contentList;
    }
    
    public static List<Map<String, Object>> getAllSubscriptions() throws SQLException {
        List<Map<String, Object>> subscriptions = new ArrayList<>();
        String sql = "SELECT s.id, s.user_id, s.type, s.status, s.amount, s.start_date, s.end_date, u.email, u.full_name " +
                     "FROM subscriptions s JOIN users u ON s.user_id = u.id ORDER BY s.start_date DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> sub = new HashMap<>();
                sub.put("id", rs.getInt("id"));
                sub.put("user_id", rs.getInt("user_id"));
                sub.put("email", rs.getString("email"));
                sub.put("full_name", rs.getString("full_name"));
                sub.put("type", rs.getString("type"));
                sub.put("status", rs.getString("status"));
                sub.put("amount", rs.getDouble("amount"));
                sub.put("start_date", rs.getTimestamp("start_date"));
                sub.put("end_date", rs.getTimestamp("end_date"));
                subscriptions.add(sub);
            }
        }
        return subscriptions;
    }
    
    public static void updateUserProfile(int userId, String fullName, String phone) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, phone_number = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, fullName);
            pstmt.setString(2, phone);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
        }
    }
    
    public static void updateUserPassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }
    
    // Subscription operations
    public static void createSubscription(int userId, String type, double amount) throws SQLException {
        String sql = "INSERT INTO subscriptions (user_id, type, status, amount, start_date, end_date) VALUES (?, ?, 'PENDING', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 month')";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, type);
            pstmt.setDouble(3, amount);
            pstmt.executeUpdate();
        }
    }
    
    public static Map<String, Object> getUserSubscription(int userId) throws SQLException {
        String sql = "SELECT * FROM subscriptions WHERE user_id = ? AND status = 'ACTIVE' ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> sub = new HashMap<>();
                sub.put("id", rs.getInt("id"));
                sub.put("type", rs.getString("type"));
                sub.put("status", rs.getString("status"));
                sub.put("end_date", rs.getTimestamp("end_date"));
                return sub;
            }
        }
        return null;
    }
    
    // Content operations
    public static List<Map<String, Object>> getAllContent(String type, String category) throws SQLException {
        List<Map<String, Object>> contentList = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name as creator FROM content c LEFT JOIN users u ON c.created_by = u.id WHERE 1=1";
        
        if (type != null && !type.isEmpty()) {
            sql += " AND c.type = ?";
        }
        if (category != null && !category.isEmpty()) {
            sql += " AND c.category = ?";
        }
        sql += " ORDER BY c.created_at DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int paramIndex = 1;
            if (type != null && !type.isEmpty()) {
                pstmt.setString(paramIndex++, type);
            }
            if (category != null && !category.isEmpty()) {
                pstmt.setString(paramIndex++, category);
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> content = new HashMap<>();
                content.put("id", rs.getInt("id"));
                content.put("title", rs.getString("title"));
                content.put("description", rs.getString("description"));
                content.put("type", rs.getString("type"));
                content.put("content_url", rs.getString("content_url"));
                content.put("body", rs.getString("body"));
                content.put("category", rs.getString("category"));
                content.put("creator", rs.getString("creator"));
                content.put("created_at", rs.getTimestamp("created_at"));
                contentList.add(content);
            }
        }
        return contentList;
    }
    
    public static int getContentCount(String type, String search) throws SQLException {
        String sql = "SELECT COUNT(*) FROM content c WHERE 1=1";
        
        if (type != null && !type.isEmpty()) {
            sql += " AND c.type = ?";
        }
        if (search != null && !search.isEmpty()) {
            sql += " AND (c.title ILIKE ? OR c.description ILIKE ? OR c.body ILIKE ? OR c.category ILIKE ?)";
        }
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int paramIndex = 1;
            if (type != null && !type.isEmpty()) {
                pstmt.setString(paramIndex++, type);
            }
            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search + "%";
                pstmt.setString(paramIndex++, searchPattern);
                pstmt.setString(paramIndex++, searchPattern);
                pstmt.setString(paramIndex++, searchPattern);
                pstmt.setString(paramIndex++, searchPattern);
            }
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    public static int getContentCountWithFilters(String type, String category, String search) throws SQLException {
        String sql = "SELECT COUNT(*) FROM content c WHERE 1=1";
        
        if (type != null && !type.isEmpty()) {
            sql += " AND c.type = ?";
        }
        if (category != null && !category.isEmpty()) {
            sql += " AND c.category = ?";
        }
        if (search != null && !search.isEmpty()) {
            sql += " AND (c.title ILIKE ? OR c.description ILIKE ? OR c.body ILIKE ? OR c.category ILIKE ?)";
        }
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int paramIndex = 1;
            if (type != null && !type.isEmpty()) {
                pstmt.setString(paramIndex++, type);
            }
            if (category != null && !category.isEmpty()) {
                pstmt.setString(paramIndex++, category);
            }
            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search + "%";
                pstmt.setString(paramIndex++, searchPattern);
                pstmt.setString(paramIndex++, searchPattern);
                pstmt.setString(paramIndex++, searchPattern);
                pstmt.setString(paramIndex++, searchPattern);
            }
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    public static List<Map<String, Object>> getAllContentPaginated(String type, String category, String search, int limit, int offset) throws SQLException {
        List<Map<String, Object>> contentList = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name as creator FROM content c LEFT JOIN users u ON c.created_by = u.id WHERE 1=1";
        
        if (type != null && !type.isEmpty()) {
            sql += " AND c.type = ?";
        }
        if (category != null && !category.isEmpty()) {
            sql += " AND c.category = ?";
        }
        if (search != null && !search.isEmpty()) {
            sql += " AND (c.title ILIKE ? OR c.description ILIKE ? OR c.body ILIKE ? OR c.category ILIKE ?)";
        }
        sql += " ORDER BY c.created_at DESC LIMIT ? OFFSET ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int paramIndex = 1;
            if (type != null && !type.isEmpty()) {
                pstmt.setString(paramIndex++, type);
            }
            if (category != null && !category.isEmpty()) {
                pstmt.setString(paramIndex++, category);
            }
            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search + "%";
                pstmt.setString(paramIndex++, searchPattern);
                pstmt.setString(paramIndex++, searchPattern);
                pstmt.setString(paramIndex++, searchPattern);
                pstmt.setString(paramIndex++, searchPattern);
            }
            pstmt.setInt(paramIndex++, limit);
            pstmt.setInt(paramIndex++, offset);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> content = new HashMap<>();
                content.put("id", rs.getInt("id"));
                content.put("title", rs.getString("title"));
                content.put("description", rs.getString("description"));
                content.put("type", rs.getString("type"));
                content.put("content_url", rs.getString("content_url"));
                content.put("body", rs.getString("body"));
                content.put("category", rs.getString("category"));
                content.put("creator", rs.getString("creator"));
                content.put("created_at", rs.getTimestamp("created_at"));
                contentList.add(content);
            }
        }
        return contentList;
    }
    
    public static Map<String, Object> getContentById(int id) throws SQLException {
        String sql = "SELECT c.*, u.full_name as creator FROM content c LEFT JOIN users u ON c.created_by = u.id WHERE c.id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> content = new HashMap<>();
                content.put("id", rs.getInt("id"));
                content.put("title", rs.getString("title"));
                content.put("description", rs.getString("description"));
                content.put("type", rs.getString("type"));
                content.put("content_url", rs.getString("content_url"));
                content.put("body", rs.getString("body"));
                content.put("category", rs.getString("category"));
                content.put("creator", rs.getString("creator"));
                content.put("created_at", rs.getTimestamp("created_at"));
                return content;
            }
        }
        return null;
    }
    
    public static void addContent(String title, String description, String type, String contentUrl, String body, String category, int createdBy) throws SQLException {
        String sql = "INSERT INTO content (title, description, type, content_url, body, category, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, description);
            pstmt.setString(3, type);
            pstmt.setString(4, contentUrl);
            pstmt.setString(5, body);
            pstmt.setString(6, category);
            pstmt.setInt(7, createdBy);
            pstmt.executeUpdate();
        }
    }
    
    public static void insertSampleContent() throws SQLException {
        // Check if content already exists
        String checkSql = "SELECT COUNT(*) FROM content";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Content already exists
            }
        }
        
        // Insert sample notes - Expanded with more topics
        String[] notes = {
            // Programming (10 notes)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Introduction to Programming', 'Learn the basics of programming', 'NOTE', 'Programming is the process of creating instructions for a computer. Key Concepts: 1. Variables 2. Data Types 3. Control Flow 4. Loops 5. Functions', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Variables and Data Types', 'Understanding variables and data types', 'NOTE', 'Variables are containers for storing data values. Primitive types: int, double, char, boolean. Reference types: String, Arrays, Objects.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Control Flow Statements', 'If-else, switch, and loop statements', 'NOTE', 'Control flow statements control execution order: If-Else, Switch, For Loop, While Loop, Do-While Loop.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Functions and Methods', 'Creating reusable code blocks', 'NOTE', 'Functions are reusable code blocks that perform specific tasks. They accept parameters, return values, and can be called multiple times throughout a program.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Object-Oriented Programming', 'OOP concepts and principles', 'NOTE', 'OOP has four pillars: Encapsulation (data hiding), Inheritance (reusability), Polymorphism (many forms), Abstraction (hiding complexity).', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Exception Handling', 'Managing errors in code', 'NOTE', 'Exception handling manages runtime errors using try-catch blocks. Types: Checked (compile-time) and Unchecked (runtime) exceptions.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('File I/O Operations', 'Reading and writing files', 'NOTE', 'File I/O allows reading from and writing to files. Key classes: FileReader, FileWriter, BufferedReader, BufferedWriter.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Recursion', 'Function calling itself', 'NOTE', 'Recursion is when a function calls itself. Base case stops recursion, recursive case continues. Used in tree traversal, factorial, Fibonacci.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Interfaces and Abstract Classes', 'Defining contracts in Java', 'NOTE', 'Abstract classes can have abstract and concrete methods. Interfaces define contracts (pre-Java 8: only abstract methods). Multiple inheritance via interfaces.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Java Collections Framework', 'Data structures in Java', 'NOTE', 'JCF includes: List (ArrayList, LinkedList), Set (HashSet, TreeSet), Map (HashMap, TreeMap), Queue (PriorityQueue, Deque).', 'PROGRAMMING', 1)",
            
            // Data Structures (10 notes)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Introduction to Arrays', 'Understanding arrays in programming', 'NOTE', 'An array is a collection of elements of the same type. Fixed size, zero-indexed, contiguous memory, fast random access O(1).', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Linked Lists', 'Understanding linked list data structure', 'NOTE', 'A linked list is a linear data structure where elements are stored in nodes. Types: Singly, Doubly, Circular. Operations: Insert O(1), Delete O(1), Search O(n).', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Stacks and Queues', 'LIFO and FIFO data structures', 'NOTE', 'Stack: LIFO (Last In First Out), push/pop O(1). Queue: FIFO (First In First Out), enqueue/dequeue O(1).', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Trees - Binary and BST', 'Hierarchical data structures', 'NOTE', 'Binary Tree: each node has up to 2 children. Binary Search Tree: left < root < right. Operations: Insert, Delete, Search O(log n) average.', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Hash Tables', 'Key-value pairs with fast lookup', 'NOTE', 'Hash table uses hash function to map keys to values. Average O(1) lookup. Collision handling: Chaining (linked lists) and Open Addressing (linear/quadratic probing).', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Heaps and Priority Queues', 'Tree-based structures', 'NOTE', 'Heap is a complete binary tree with heap property. Max-Heap: parent > children. Min-Heap: parent < children. Used in priority queues and heap sort.', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Graphs - Adjacency List and Matrix', 'Representing relationships', 'NOTE', 'Graph: nodes (vertices) connected by edges. Representations: Adjacency List (space O(V+E)), Adjacency Matrix (space O(V^2)).', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Tries (Prefix Trees)', 'String-based data structure', 'NOTE', 'Trie stores strings efficiently. Each node represents a character. Used for autocomplete, IP routing,spell checking. Space O(ALPHABET_SIZE * #keys * avgLength).', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('AVL Trees and Rotations', 'Self-balancing BST', 'NOTE', 'AVL tree maintains balance factor (height difference of subtrees <= 1). Rotations: Left, Right, Left-Right, Right-Left. Guarantees O(log n) operations.', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Red-Black Trees', 'Balanced binary search trees', 'NOTE', 'Red-Black Tree: nodes colored red/black, root black, red nodes have black children, all paths have same black height. O(log n) operations.', 'DATA_STRUCTURES', 1)",
            
            // Algorithms (10 notes)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Big O Notation', 'Understanding algorithm complexity', 'NOTE', 'Big O notation describes algorithm performance: O(1) Constant, O(log n) Logarithmic, O(n) Linear, O(n log n) Linearithmic, O(n^2) Quadratic, O(2^n) Exponential.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Sorting Algorithms', 'Common sorting techniques', 'NOTE', 'Sorting arranges elements in order. Bubble Sort O(n^2), Selection Sort O(n^2), Insertion Sort O(n^2), Quick Sort O(n log n) avg, Merge Sort O(n log n).', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Search Algorithms', 'Linear and binary search', 'NOTE', 'Linear Search: O(n) check each element. Binary Search: O(log n) requires sorted array, divide search space in half each step.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Depth-First Search (DFS)', 'Graph traversal technique', 'NOTE', 'DFS explores as far as possible before backtracking. Uses stack (or recursion). Time O(V+E), Space O(V). Used for path finding, cycle detection.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Breadth-First Search (BFS)', 'Level-by-level graph traversal', 'NOTE', 'BFS explores level by level using queue. Time O(V+E), Space O(V). Shortest path in unweighted graphs. Used for level-order traversal.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Dynamic Programming', 'Memoization and tabulation', 'NOTE', 'DP solves problems by breaking into overlapping subproblems. Approaches: Top-down (memoization), Bottom-up (tabulation). Examples: Fibonacci, Knapsack.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Greedy Algorithms', 'Local optimal choices', 'NOTE', 'Greedy makes local optimal choice hoping for global optimum. Examples: Huffman coding, Dijkstra, Kruskal, Activity selection. Not always correct.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Divide and Conquer', 'Split, solve, combine', 'NOTE', 'Divide problem into subproblems, solve recursively, combine results. Examples: Merge Sort, Quick Sort, Binary Search, Closest Pair of Points.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Dijkstras Algorithm', 'Shortest path in weighted graphs', 'NOTE', 'Dijkstra finds shortest path from source to all vertices. Uses priority queue. Time O((V+E) log V). Does not work with negative weights.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Minimum Spanning Tree', 'Kruskal and Prim algorithms', 'NOTE', 'MST connects all vertices with minimum total edge weight. Kruskal: sort edges, add if no cycle (Union-Find). Prim: grow tree from starting vertex.', 'ALGORITHMS', 1)",
            
            // Databases (10 notes)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Introduction to SQL', 'Database query language basics', 'NOTE', 'SQL manages relational databases. Commands: DDL (CREATE, ALTER, DROP), DML (SELECT, INSERT, UPDATE, DELETE), DCL (GRANT, REVOKE).', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('SQL SELECT and Filtering', 'Querying data', 'NOTE', 'SELECT retrieves data. WHERE filters rows. ORDER BY sorts. LIMIT restricts rows. DISTINCT removes duplicates. Aliases rename columns.', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('SQL JOINs', 'Combining tables', 'NOTE', 'JOINs combine rows from tables. Types: INNER (common), LEFT (all left), RIGHT (all right), FULL (all), CROSS (cartesian).', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('SQL Aggregations', 'Grouping and aggregating', 'NOTE', 'Aggregate functions: COUNT, SUM, AVG, MIN, MAX. GROUP BY groups rows. HAVING filters groups. ORDER BY sorts results.', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Database Normalization', 'Organizing data efficiently', 'NOTE', 'Normalization reduces redundancy. 1NF: atomic values. 2NF: no partial dependencies. 3NF: no transitive dependencies. BCNF: stronger 3NF.', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Indexes and Performance', 'Optimizing queries', 'NOTE', 'Indexes speed up data retrieval. Types: B-Tree (default), Hash, Composite. Pros: faster reads. Cons: slower writes, more storage.', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Transactions and ACID', 'Ensuring data integrity', 'NOTE', 'Transactions are atomic units. ACID: Atomicity (all or nothing), Consistency (valid state), Isolation (concurrent safe), Durability (persisted).', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('NoSQL Databases', 'Non-relational databases', 'NOTE', 'NoSQL types: Document (MongoDB), Key-Value (Redis), Column (Cassandra), Graph (Neo4j). Advantages: scalability, flexibility, performance.', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('SQL Subqueries', 'Nested queries', 'NOTE', 'Subquery is query within another query. Types: Scalar (returns single value), Table (returns table), Correlated (references outer query).', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Database Security', 'Protecting data', 'NOTE', 'Security measures: Authentication, Authorization, Encryption (at rest/transit), SQL injection prevention, Principle of least privilege.', 'DATABASES', 1)",
            
            // Web Development (10 notes)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('HTML Fundamentals', 'Building web structure', 'NOTE', 'HTML provides structure. Tags: <html>, <head>, <body>, <div>, <span>, <p>, <a>, <img>, <table>, <form>. Semantic HTML5: <header>, <nav>, <article>.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('CSS Basics', 'Styling web pages', 'NOTE', 'CSS styles elements. Selectors: element, class, id. Properties: color, margin, padding, display, position, flexbox, grid. Responsive design with media queries.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('JavaScript Essentials', 'Adding interactivity', 'NOTE', 'JavaScript adds behavior. Variables: var, let, const. Functions: regular and arrow. DOM manipulation. Events. ES6+ features: spread, destructuring, promises.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('HTTP Protocol', 'Web communication', 'NOTE', 'HTTP is request-response protocol. Methods: GET (retrieve), POST (create), PUT (update), DELETE (remove). Status codes: 200s (success), 400s (client error), 500s (server error).', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('RESTful APIs', 'Web service architecture', 'NOTE', 'REST uses HTTP methods. Resources: /users, /posts. CRUD: Create=POST, Read=GET, Update=PUT/PATCH, Delete=DELETE. JSON format. Stateless.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('React.js Introduction', 'Building user interfaces', 'NOTE', 'React is a component-based UI library. JSX combines HTML and JavaScript. Props pass data. State manages internal data. Hooks: useState, useEffect.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Node.js and Express', 'Server-side JavaScript', 'NOTE', 'Node.js runs JavaScript on server. Express is minimal web framework. Middleware functions process requests. Routes map URLs to handlers.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Authentication and JWT', 'Secure user sessions', 'NOTE', 'JWT (JSON Web Token) stores user info. Parts: header, payload, signature. Sent in Authorization header. Stateless, scalable authentication.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('CSS Flexbox', 'Modern layout technique', 'NOTE', 'Flexbox creates flexible layouts. Container: display: flex. Properties: flex-direction, justify-content, align-items, gap. Items: flex-grow, flex-shrink, flex-basis.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Responsive Design', 'Mobile-friendly websites', 'NOTE', 'Responsive design adapts to screen sizes. Techniques: fluid grids, flexible images, media queries. Breakpoints at 768px (tablet), 1024px (desktop).', 'WEB_DEVELOPMENT', 1)",
            
            // MS Word (10 notes)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Introduction to MS Word', 'Word processing basics', 'NOTE', 'MS Word is a word processing application by Microsoft. It is used to create, edit, format, and print text documents. Key features: text formatting, spell check, templates, mail merge, tables, headers/footers.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Text Formatting in Word', 'Font and paragraph formatting', 'NOTE', 'Text formatting includes: Font (typeface, size, color), Bold (Ctrl+B), Italic (Ctrl+I), Underline (Ctrl+U), Alignment (left, center, right, justify), Line spacing, and Paragraph indentation.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Working with Tables', 'Creating and formatting tables', 'NOTE', 'Tables organize data in rows and columns. Insert via Insert > Table. Format with Table Design tab. Operations: merge cells, split cells, sort data, apply formulas. Auto-fit adjusts column width.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Headers, Footers and Page Numbers', 'Document sections', 'NOTE', 'Headers appear at the top of every page, footers at the bottom. Page numbers can be inserted in either. Double-click header/footer area to edit. Different first page option available.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Mail Merge', 'Bulk document creation', 'NOTE', 'Mail Merge creates personalized documents from a template and data source. Steps: 1. Select document type (letter, envelope, label). 2. Select recipients (data source). 3. Insert merge fields. 4. Preview and complete merge.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Page Setup and Layout', 'Document layout controls', 'NOTE', 'Page Setup includes: Orientation (portrait/landscape), Size (A4, Letter, Legal), Margins (normal, narrow, wide), Columns (1, 2, 3). Found under Layout tab.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Spell Check and Grammar', 'Error detection tools', 'NOTE', 'Spell check detects misspelled words (red underline) and grammar errors (blue underline). AutoCorrect fixes common mistakes. Right-click for suggestions. Review > Spelling and Grammar for full check.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Track Changes and Comments', 'Collaboration features', 'NOTE', 'Track Changes records all edits (insertions in red, deletions in strikethrough). Comments add notes without altering text. Review tab: Accept, Reject, Next, Previous. Show Markup to filter.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Styles and Themes', 'Consistent formatting', 'NOTE', 'Styles are predefined formatting sets (Heading 1, Normal, Title). Themes apply coordinated colors, fonts, and effects. Styles ensure consistency. Modify styles via Modify Style dialog.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Inserting Objects', 'Images, shapes, and charts', 'NOTE', 'Insert tab allows adding: Pictures (from file/online), Shapes (rectangles, arrows), Icons, Charts, SmartArt, Text boxes, WordArt. Objects can be resized, moved, and wrapped with text.', 'MS_WORD', 1)",
            
            // MS Excel (10 notes)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Introduction to MS Excel', 'Spreadsheet basics', 'NOTE', 'MS Excel is a spreadsheet application for organizing, calculating, and analyzing data. Structure: Workbook > Worksheets > Cells. Each cell has a unique address (e.g., A1). Used for budgets, data analysis, charts.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Cell References and Ranges', 'Identifying cells', 'NOTE', 'Cell reference: Column letter + Row number (A1). Range: Start cell:End cell (A1:D10). Types: Relative (A1 - changes when copied), Absolute ($A$1 - fixed), Mixed ($A1 or A$1). Named ranges give descriptive names.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Formulas in Excel', 'Performing calculations', 'NOTE', 'Formulas start with = and use operators: + (add), - (subtract), * (multiply), / (divide), ^ (power). Can reference cells and combine with functions. Example: =A1+B1*2. Order of operations applies (BODMAS).', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Common Functions', 'Built-in formulas', 'NOTE', 'SUM(range): adds values. AVERAGE(range): calculates mean. COUNT(range): counts numbers. MAX(range): largest value. MIN(range): smallest. IF(condition, true, false): conditional. SUMIF, COUNTIF for criteria-based operations.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Charts and Graphs', 'Data visualization', 'NOTE', 'Charts visualize data. Types: Column/Bar (comparison), Line (trends), Pie (proportions), Scatter (correlations), Area (volume). Insert via Insert > Chart. Customize with Chart Design and Format tabs.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Sorting and Filtering', 'Organizing data', 'NOTE', 'Sorting arranges data ascending/descending. Data > Sort (multi-level sorting). Filtering shows specific rows using Data > Filter (dropdown arrows). AutoFilter, Custom Filter, and Advanced Filter available.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Conditional Formatting', 'Visual data formatting', 'NOTE', 'Conditional formatting applies colors/format based on cell values. Rules: Highlight Cell Rules (greater than, less than, between), Top/Bottom Rules, Data Bars, Color Scales, Icon Sets. Found in Home > Conditional Formatting.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('VLOOKUP and HLOOKUP', 'Lookup functions', 'NOTE', 'VLOOKUP(lookup_value, table_array, col_index, [range_lookup]) searches vertically. HLOOKUP searches horizontally. Both return value from matching row/column. FALSE for exact match, TRUE for approximate.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Data Validation', 'Input control', 'NOTE', 'Data Validation restricts what can be entered in cells. Data > Validation. Allow: Whole number, Decimal, List, Date, Time, Text length. Input message and error alert can be set. Drop-down lists created from ranges.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Pivot Tables', 'Data summarization', 'NOTE', 'Pivot Tables summarize large data sets. Fields: Filters, Columns, Rows, Values. Drag fields to areas to analyze. Can group, sort, filter, and calculate. Insert > PivotTable from selected data range.', 'MS_EXCEL', 1)",
            
            // MS Access (10 notes)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Introduction to MS Access', 'Database basics', 'NOTE', 'MS Access is a relational database management system (RDBMS). Components: Tables (store data), Queries (retrieve data), Forms (data entry), Reports (print output), Macros (automation). Used for small to medium databases.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Creating and Designing Tables', 'Table structure', 'NOTE', 'Tables store data in records (rows) and fields (columns). Design View defines: Field Name, Data Type (Text, Number, Date/Time, Yes/No, AutoNumber, Memo), Field Properties (size, format, validation). Primary key required.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Queries in Access', 'Data retrieval and manipulation', 'NOTE', 'Queries retrieve, add, update, or delete data. Select Query: displays data matching criteria. Action Queries: Make-Table, Append, Update, Delete. SQL View for manual SQL. Design View with criteria grid.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Forms for Data Entry', 'User interface', 'NOTE', 'Forms provide user-friendly data entry. Created via Form Wizard or Form Design. Controls: Text Box, Label, Combo Box, Check Box, Button, Subform. Properties: Caption, Default Value, Validation Rule.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Reports for Printing', 'Formatted output', 'NOTE', 'Reports present data in formatted, printable layout. Sections: Report Header/Footer, Page Header/Footer, Detail. Grouping and sorting organize data. Calculations: Sum, Avg, Count, Min, Max.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Primary Keys and Relationships', 'Database connections', 'NOTE', 'Primary Key: uniquely identifies records (no duplicates, no null). Foreign Key: references another table primary key. Relationships: One-to-One, One-to-Many, Many-to-Many. Enforce Referential Integrity for data consistency.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Data Types in Access', 'Field data types', 'NOTE', 'Data types: Short Text (255 chars), Long Text (64KB), Number (Byte, Integer, Long, Single, Double), Date/Time, Currency, AutoNumber, Yes/No, OLE Object, Hyperlink, Attachment, Calculated.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Sorting and Filtering Data', 'Organizing records', 'NOTE', 'Sort records A-Z or Z-A on any field. Filter by Selection (click value), Filter by Form (enter criteria), Advanced Filter (complex criteria). Toggle filter to show/hide filtered results.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Macros in Access', 'Automation without code', 'NOTE', 'Macros automate repetitive tasks. Actions: OpenForm, OpenQuery, OpenReport, Close, SetValue, MsgBox, RunMacro. Conditions control when actions execute. Macro Designer provides visual interface.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Importing and Exporting Data', 'Data transfer', 'NOTE', 'Import: External Data > New Data Source (Excel, CSV, other databases). Export: Right-click table > Export (Excel, PDF, Word, CSV). Link tables connect to external data without importing.', 'MS_ACCESS', 1)",
            
            // MS PowerPoint (10 notes)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Introduction to MS PowerPoint', 'Presentation basics', 'NOTE', 'MS PowerPoint creates slide-based presentations. Features: slides, text, images, animations, transitions, speaker notes. Used for lectures, business meetings, conferences. File extension: .pptx.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Creating and Editing Slides', 'Slide management', 'NOTE', 'New Slide (Ctrl+M) adds slides. Slide layouts: Title Slide, Title and Content, Two Content, Comparison, Blank. Duplicate, delete, reorder slides. Slide Sorter view for reorganizing.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Adding Content to Slides', 'Text, images, and media', 'NOTE', 'Text: Click placeholder or insert text box. Images: Insert > Pictures (file, online, stock). Shapes: Insert > Shapes. Icons, 3D Models, SmartArt, Charts, Tables. Audio and Video insertion available.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Slide Transitions', 'Between-slide effects', 'NOTE', 'Transitions animate between slides. Types: Subtle (Fade), Exciting (Cube, Origami), Dynamic Content. Set Duration, Sound, and Advance options. Apply to one slide or all slides. Transitions tab.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Animations', 'Element motion effects', 'NOTE', 'Animations add motion to slide elements. Entrance (Fly In), Emphasis (Spin), Exit (Fly Out), Motion Paths. Animation Pane controls order and timing. Trigger options start animations on click or automatically.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Slide Master', 'Global design control', 'NOTE', 'Slide Master (View > Slide Master) controls design of all slides. Edit master slide for universal changes. Layout masters customize specific layouts. Add logos, fonts, colors once, apply everywhere.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Themes and Design Ideas', 'Presentation styling', 'NOTE', 'Themes provide coordinated colors, fonts, and effects. Design tab offers built-in themes. Design Ideas suggests layouts based on content. Variants modify current theme colors and fonts.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Presenting a Slide Show', 'Delivery methods', 'NOTE', 'F5 starts from beginning, Shift+F5 from current slide. Presenter View: notes, timer, next slide on speaker screen. Navigation: arrows, spacebar, click. Escape exits. Kiosk mode for unattended display.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('SmartArt and Diagrams', 'Visual information graphics', 'NOTE', 'SmartArt creates professional diagrams. Types: List (bullet points), Process (steps), Cycle (circular), Hierarchy (org chart), Relationship (connections), Matrix (quadrant), Pyramid (hierarchy).', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Hyperlinks and Action Buttons', 'Interactive presentations', 'NOTE', 'Hyperlinks navigate to slides, files, or websites. Insert > Hyperlink (Ctrl+K). Action Buttons trigger actions on click or hover. Used for interactive quizzes, navigation menus, branching presentations.', 'MS_POWERPOINT', 1)",
            
            // MS Publisher (10 notes)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Introduction to MS Publisher', 'Desktop publishing basics', 'NOTE', 'MS Publisher is a desktop publishing application for creating professional publications. Unlike Word, it focuses on page layout and design. Creates: brochures, newsletters, flyers, business cards, calendars, postcards.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Working with Templates', 'Starting publications', 'NOTE', 'Publisher provides templates for quick starts: Brochures, Newsletters, Flyers, Business Cards, Invitations, Certificates. Templates include placeholder text and images. Customize by replacing content.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Text Boxes and Typography', 'Text management', 'NOTE', 'Text boxes contain and control text placement. Insert > Draw Text Box. Link text boxes so text flows between them. Format: font, size, color, alignment, columns. Text wrapping around images.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Working with Images', 'Image placement and editing', 'NOTE', 'Insert > Pictures adds images. Picture Tools: Crop, Corrections, Color, Artistic Effects, Compress, Wrap Text. Align images with rulers and guides. Insert > Online Pictures for stock images.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Page Layout and Design', 'Publication structure', 'NOTE', 'Page Design tab controls layout: Size, Margins, Columns, Guides, Rulers. Layout Guides define columns and margins. Arrange objects with Align, Rotate, Order (Bring to Front, Send to Back).', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Building Blocks', 'Reusable elements', 'NOTE', 'Building Blocks are pre-made design elements: Page Parts (headers, sidebars), Borders, Calendars, Advertisements, Business Information. Insert from Building Block Library. Save custom elements.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Color Schemes and Design', 'Visual consistency', 'NOTE', 'Color schemes define coordinated publication colors: Main, Accent 1-5, Background. Apply from Page Design > Schemes. Custom schemes can be created. Professional design follows color theory principles.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Mail Merge in Publisher', 'Personalized publications', 'NOTE', 'Mail Merge creates personalized publications from a data source. Steps: 1. Insert merge fields. 2. Connect to data source (Excel, Access, Outlook). 3. Preview merged results. 4. Print or email merged publications.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Printing and Pack and Go', 'Publication output', 'NOTE', 'Print: File > Print with options for copies, pages, paper size, print quality. Pack and Go: File > Pack and Go > Take to Commercial Printing Service. Creates compressed file with all linked resources included.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Design Tips and Best Practices', 'Professional publishing', 'NOTE', 'Best practices: Use consistent fonts (2-3 max), maintain white space, align elements to grid, use high-resolution images (300 DPI for print), proofread carefully, follow visual hierarchy (headlines > subheadings > body).', 'MS_PUBLISHER', 1)",

            // Office Applications (10 notes)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Introduction to Office Applications', 'Overview of Microsoft Office suite', 'NOTE', 'Microsoft Office is a suite of productivity applications. Core apps: Word (documents), Excel (spreadsheets), PowerPoint (presentations), Access (databases), Publisher (desktop publishing). Used worldwide in business, education, and personal productivity.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Microsoft Word Essentials', 'Word processing fundamentals', 'NOTE', 'MS Word is used for creating and editing text documents. Key features: text formatting, spell check, tables, mail merge, headers/footers, page layout, styles, and templates. Keyboard shortcuts: Ctrl+B (bold), Ctrl+I (italic), Ctrl+S (save).', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Microsoft Excel Fundamentals', 'Spreadsheet basics', 'NOTE', 'MS Excel organizes data in rows and columns. Key features: formulas, functions (SUM, AVERAGE, VLOOKUP), charts, pivot tables, conditional formatting, data validation, and sorting/filtering. Essential for data analysis and financial calculations.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Microsoft PowerPoint Basics', 'Creating presentations', 'NOTE', 'MS PowerPoint creates slide-based presentations. Key features: slide layouts, themes, animations, transitions, speaker notes, SmartArt, charts, and multimedia. Best practices: minimal text per slide, consistent design, use visuals effectively.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Microsoft Access Introduction', 'Database management basics', 'NOTE', 'MS Access is a relational database management system. Components: tables (store data), queries (retrieve data), forms (data entry), reports (print output). Used for managing small to medium databases with relationships between tables.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Microsoft Publisher Overview', 'Desktop publishing basics', 'NOTE', 'MS Publisher creates professional publications: brochures, newsletters, flyers, business cards, and calendars. Focuses on page layout and design rather than text editing. Features: templates, text boxes, image placement, and mail merge.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Office File Management', 'Managing Office documents', 'NOTE', 'File management in Office: Save (Ctrl+S), Save As, AutoSave. File formats: .docx (Word), .xlsx (Excel), .pptx (PowerPoint), .accdb (Access). Cloud storage: OneDrive integration for automatic saving and sharing.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Cross-Application Integration', 'Working between Office apps', 'NOTE', 'Office apps integrate seamlessly: Copy-paste between apps, embed Excel charts in Word/PPT, link Access data to Excel, create mail merge from Excel/Access data. Paste Special preserves formatting. Object linking maintains live updates.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Office Collaboration Features', 'Working together in Office', 'NOTE', 'Collaboration features: Track Changes (Word), Comments, Shared workbooks (Excel), Co-authoring (simultaneous editing), Version History, Share button for OneDrive/SharePoint. Real-time collaboration with cloud-based Office 365.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Office Productivity Tips', 'Efficiency in Office', 'NOTE', 'Productivity tips: Use keyboard shortcuts (Ctrl+C, Ctrl+V, Ctrl+Z), Quick Access Toolbar customization, Ribbon navigation, Format Painter, Find and Replace (Ctrl+H), Templates for consistency, AutoCorrect for common corrections.', 'OFFICE_APPLICATIONS', 1)",

            // Internet and Email (10 notes)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Introduction to the Internet', 'Global network basics', 'NOTE', 'The Internet is a global network connecting millions of computers using TCP/IP protocol. Services: World Wide Web (WWW), Email, File Transfer (FTP), Instant Messaging, Video Conferencing. Started as ARPANET in 1969.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Web Browsers', 'Accessing the web', 'NOTE', 'Web browsers access and display websites. Popular: Google Chrome, Mozilla Firefox, Microsoft Edge, Apple Safari. Features: tabs, bookmarks, history, downloads, private browsing, extensions. Render HTML/CSS/JavaScript.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Search Engines', 'Finding information', 'NOTE', 'Search engines index web pages and provide search results. Popular: Google, Bing, Yahoo, DuckDuckGo. Techniques: keywords, phrases (quotes), site: operator, - (exclude), OR. Boolean operators refine searches.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Email Basics', 'Electronic mail', 'NOTE', 'Email (Electronic Mail) sends messages between users. Components: To, CC (Carbon Copy), BCC (Blind Carbon Copy), Subject, Body, Attachments. Providers: Gmail, Outlook, Yahoo Mail. Protocols: SMTP (send), IMAP/POP3 (receive).', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('URL and Domain Names', 'Web addresses', 'NOTE', 'URL (Uniform Resource Locator) identifies web resources. Structure: protocol (https://) + domain (www.example.com) + path (/page) + parameters (?id=1). Domain hierarchy: TLD (.com, .org, .ke) + domain name + subdomain (www).', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Internet Safety and Security', 'Online protection', 'NOTE', 'Safety measures: Use strong passwords, enable two-factor authentication, avoid suspicious links, use HTTPS sites, keep software updated, use antivirus, be cautious with personal information, recognize phishing attempts.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Social Media and Networking', 'Online communities', 'NOTE', 'Social media platforms: Facebook (social networking), Twitter/X (microblogging), LinkedIn (professional), Instagram (photo sharing), YouTube (video sharing). Privacy settings, digital footprint, responsible posting important.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Cloud Storage and Services', 'Online file management', 'NOTE', 'Cloud storage saves files online: Google Drive (15GB free), Dropbox (2GB free), OneDrive (5GB free), iCloud. Benefits: access anywhere, automatic backup, file sharing, collaboration. Sync between devices.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Wi-Fi and Networking', 'Wireless connections', 'NOTE', 'Wi-Fi connects devices wirelessly to a network. Standards: 802.11a/b/g/n/ac/ax (Wi-Fi 6). Setup: router configuration, SSID (network name), password (WPA2/WPA3 security). Hotspots provide public Wi-Fi access.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Netiquette and Digital Citizenship', 'Online behavior', 'NOTE', 'Netiquette (Internet etiquette): Be respectful, avoid ALL CAPS (shouting), do not spam, respect privacy, cite sources, proofread before sending. Digital citizenship: responsible use, digital literacy, online ethics.', 'INTERNET_EMAIL', 1)",
            
            // Emerging Trends (10 notes)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Artificial Intelligence (AI)', 'Machine intelligence', 'NOTE', 'AI simulates human intelligence in machines. Subfields: Machine Learning, Natural Language Processing (NLP), Computer Vision, Expert Systems, Robotics. Applications: virtual assistants, recommendation systems, autonomous vehicles.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Machine Learning (ML)', 'Data-driven learning', 'NOTE', 'ML enables computers to learn from data. Types: Supervised (labeled training data), Unsupervised (finds patterns), Reinforcement (reward-based). Algorithms: Decision Trees, Neural Networks, SVM, K-Means. Applications: fraud detection, image recognition.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Internet of Things (IoT)', 'Connected devices', 'NOTE', 'IoT connects everyday objects to the Internet. Examples: smart thermostats, wearable fitness trackers, connected cars, industrial sensors. Protocols: MQTT, CoAP, Zigbee. Applications: smart homes, healthcare monitoring, agriculture.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Blockchain Technology', 'Distributed ledger', 'NOTE', 'Blockchain is a decentralized, immutable ledger. Blocks linked cryptographically. Features: transparency, security, no central authority. Applications: cryptocurrency (Bitcoin), supply chain tracking, smart contracts, digital identity.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Big Data', 'Large-scale data analysis', 'NOTE', 'Big data refers to extremely large data sets. 5 Vs: Volume (size), Velocity (speed of generation), Variety (structured/unstructured), Veracity (accuracy), Value (usefulness). Tools: Hadoop, Spark, NoSQL databases.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Cybersecurity', 'Digital protection', 'NOTE', 'Cybersecurity protects systems from attacks. Threats: malware, ransomware, phishing, DDoS, man-in-the-middle. Defenses: firewalls, encryption, antivirus, VPN, multi-factor authentication, security awareness training.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Cloud Computing', 'On-demand services', 'NOTE', 'Cloud computing delivers computing over the Internet. Models: IaaS (Infrastructure), PaaS (Platform), SaaS (Software). Providers: AWS, Azure, Google Cloud. Benefits: scalability, cost-efficiency, reliability, global access.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Virtual and Augmented Reality', 'Immersive technologies', 'NOTE', 'VR (Virtual Reality) creates fully immersive digital environments. AR (Augmented Reality) overlays digital content on real world. Devices: Oculus Rift, HTC Vive, Microsoft HoloLens. Applications: gaming, training, education, medicine.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('5G and Connectivity', 'Next-generation networks', 'NOTE', '5G is fifth-generation mobile technology. Features: faster speeds (up to 10 Gbps), lower latency (1ms), more simultaneous connections. Enables: IoT expansion, autonomous vehicles, remote surgery, smart cities, edge computing.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Quantum Computing', 'Advanced computation', 'NOTE', 'Quantum computing uses quantum mechanics (qubits instead of bits). Properties: superposition (multiple states), entanglement (correlated qubits). Potential: cryptography, drug discovery, optimization, climate modeling. Companies: IBM, Google, Microsoft.', 'EMERGING_TRENDS', 1)"
        };
        
        // Insert sample questions - Expanded
        String[] questions = {
            // Programming (10)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a variable?', 'A variable is a container for storing data values', 'SAMPLE_QUESTION', 'A variable is a container for storing data values in a program. It has a name, type, and value that can be changed during execution.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Explain int vs String', 'Difference between int and String', 'SAMPLE_QUESTION', 'int is a primitive data type for whole numbers (32-bit). String is a reference type (object) for text/characters, stored as array of chars.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a function?', 'Understanding functions', 'SAMPLE_QUESTION', 'A function is a reusable block of code that performs a specific task. It can accept inputs (parameters), execute logic, and return a value.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Explain OOP pillars', 'Four principles of OOP', 'SAMPLE_QUESTION', 'Encapsulation (data hiding), Inheritance (reusability), Polymorphism (multiple forms), Abstraction (hiding complexity).', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is an exception?', 'Error handling in code', 'SAMPLE_QUESTION', 'An exception is an event that disrupts normal program flow. Handled with try-catch blocks. Types: checked (compile-time) and unchecked (runtime).', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is recursion?', 'Function calling itself', 'SAMPLE_QUESTION', 'Recursion is when a function calls itself to solve a problem by breaking it into smaller subproblems. Requires base case to stop.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Explain abstract class vs interface', 'OOP abstractions', 'SAMPLE_QUESTION', 'Abstract class can have abstract and concrete methods, single inheritance. Interface (pre-Java 8) only abstract methods, multiple inheritance.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a constructor?', 'Object initialization', 'SAMPLE_QUESTION', 'Constructor initializes objects. Same name as class, no return type. Types: default (no-arg), parameterized, copy constructor.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Explain static keyword', 'Class-level members', 'SAMPLE_QUESTION', 'Static members belong to class, not objects. Shared across all instances. Static methods can be called without creating object.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is polymorphism?', 'Multiple forms in OOP', 'SAMPLE_QUESTION', 'Polymorphism allows objects to take many forms. Compile-time (overloading): same method different parameters. Runtime (overriding): subclass method.', 'PROGRAMMING', 1)",
            
            // Data Structures (10)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is an array?', 'Understanding arrays', 'SAMPLE_QUESTION', 'An array is a data structure that stores a collection of elements of the same type in contiguous memory. Fixed size, zero-indexed, O(1) random access.', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a linked list?', 'Linked list definition', 'SAMPLE_QUESTION', 'A linked list is a linear data structure where each element contains data and a pointer/reference to the next node. Dynamic size, O(1) insert/delete.', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Stack vs Queue', 'LIFO vs FIFO', 'SAMPLE_QUESTION', 'Stack: Last In First Out (LIFO), like a stack of plates. Operations: push, pop. Queue: First In First Out (FIFO), like a line. Operations: enqueue, dequeue.', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Binary Search Tree operations', 'BST insert, delete, search', 'SAMPLE_QUESTION', 'BST: left subtree < root < right subtree. Search: O(log n) average. Insert: follow BST property. Delete: three cases (leaf, one child, two children).', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Hash table collisions', 'Handling hash collisions', 'SAMPLE_QUESTION', 'Collision when two keys hash to same index. Solutions: Chaining (linked list at each bucket), Open Addressing (probe sequence: linear, quadratic, double hashing).', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a heap?', 'Heap data structure', 'NOTE', 'Heap is a complete binary tree with heap property. Max-Heap: parent >= children. Min-Heap: parent <= children. Used for priority queues and heap sort.', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('DFS vs BFS', 'Graph traversal comparison', 'SAMPLE_QUESTION', 'DFS: goes deep first (stack/recursion), good for path finding. BFS: goes wide first (queue), finds shortest path in unweighted graphs.', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Tree traversal methods', 'Inorder, preorder, postorder', 'SAMPLE_QUESTION', 'Inorder: left-root-right (sorted BST). Preorder: root-left-right (copy tree). Postorder: left-right-root (delete tree).', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a graph?', 'Graph data structure', 'SAMPLE_QUESTION', 'Graph: set of vertices (nodes) connected by edges. Types: directed/undirected, weighted/unweighted, cyclic/acyclic.', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Time complexity of operations', 'DS operation complexities', 'SAMPLE_QUESTION', 'Array: O(1) access, O(n) insert. LinkedList: O(n) access, O(1) insert/delete. HashMap: O(1) avg. BST: O(log n) avg. Stack/Queue: O(1) all ops.', 'DATA_STRUCTURES', 1)",
            
            // Algorithms (10)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Binary search complexity', 'Time complexity question', 'SAMPLE_QUESTION', 'O(log n) - the search space halves with each comparison. Requires sorted array. Divide and conquer approach.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Big O Notation', 'Algorithm complexity', 'SAMPLE_QUESTION', 'Big O notation describes the upper bound of an algorithm time or space complexity as input size grows. O(1), O(log n), O(n), O(n log n), O(n^2), O(2^n).', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Bubble Sort', 'Simple sorting algorithm', 'SAMPLE_QUESTION', 'Bubble Sort repeatedly steps through list, compares adjacent elements, swaps if in wrong order. Time O(n^2), Space O(1). Stable.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Quick Sort', 'Efficient sorting', 'SAMPLE_QUESTION', 'Quick Sort: pick pivot, partition (smaller left, larger right), recursively sort. Average O(n log n), worst O(n^2). Not stable.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Merge Sort', 'Divide and conquer sorting', 'SAMPLE_QUESTION', 'Merge Sort: divide array in half, sort recursively, merge sorted halves. Time O(n log n), Space O(n). Stable sort.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Dijkstras Algorithm', 'Shortest path', 'SAMPLE_QUESTION', 'Dijkstra finds shortest path from source to all nodes. Uses greedy approach with priority queue. Time O((V+E) log V). Does not work with negative weights.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Dynamic Programming example', 'Fibonacci with DP', 'SAMPLE_QUESTION', 'DP solves overlapping subproblems. Fibonacci: naive O(2^n), memoization O(n), bottom-up tabulation O(n).', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Greedy vs DP', 'Algorithm paradigms', 'SAMPLE_QUESTION', 'Greedy: local optimal choice, may not find global optimum. DP: considers all subproblems, always finds optimal (but slower).', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is binary search?', 'Search algorithm', 'SAMPLE_QUESTION', 'Binary search finds target in sorted array by repeatedly dividing search space in half. O(log n). Compare middle element, discard half.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Space complexity', 'Memory usage analysis', 'SAMPLE_QUESTION', 'Space complexity measures memory needed. In-place algorithms use O(1) extra space. Recursive algorithms include call stack space.', 'ALGORITHMS', 1)",
            
            // Databases (10)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('SQL JOIN operations', 'JOIN explanation', 'SAMPLE_QUESTION', 'JOIN combines rows from two tables based on related column. Types: INNER (matching), LEFT (all left), RIGHT (all right), FULL (all), CROSS (cartesian).', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Primary Key vs Foreign Key', 'Database keys', 'SAMPLE_QUESTION', 'Primary Key: uniquely identifies each record, one per table, no nulls. Foreign Key: references Primary Key in another table, establishes relationship.', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Database Normalization', 'Data organization', 'SAMPLE_QUESTION', 'Normalization organizes data to reduce redundancy. 1NF: atomic values. 2NF: no partial dependencies. 3NF: no transitive dependencies.', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('SQL vs NoSQL', 'Database types comparison', 'SAMPLE_QUESTION', 'SQL: relational, structured data, fixed schema, ACID. NoSQL: non-relational, flexible schema, scalable, eventual consistency.', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('ACID properties', 'Transaction properties', 'SAMPLE_QUESTION', 'ACID: Atomicity (all or nothing), Consistency (valid state), Isolation (concurrent transactions), Durability (committed data persists).', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is an index?', 'Database optimization', 'SAMPLE_QUESTION', 'Index is a data structure that speeds up data retrieval. B-Tree most common. Pros: faster reads. Cons: slower writes, more storage.', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('SQL GROUP BY', 'Grouping data', 'SAMPLE_QUESTION', 'GROUP BY groups rows with same values. Used with aggregate functions (COUNT, SUM, AVG). HAVING filters groups (WHERE filters rows).', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Denormalization', 'Database optimization', 'SAMPLE_QUERY', 'Denormalization adds redundant data to improve read performance. Opposite of normalization. Used in read-heavy systems.', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('SQL Injection', 'Security vulnerability', 'SAMPLE_QUESTION', 'SQL injection attacks insert malicious SQL code through user input. Prevention: parameterized queries, input validation, least privilege.', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Database views', 'Virtual tables', 'SAMPLE_QUESTION', 'View is a virtual table based on query result. Does not store data. Used for security (hide columns), simplify complex queries.', 'DATABASES', 1)",
            
            // Web Development (10)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('GET vs POST', 'HTTP methods', 'SAMPLE_QUESTION', 'GET retrieves data, parameters in URL (limited length), can be cached. POST sends data in request body, more secure, no caching.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('HTML vs CSS vs JavaScript', 'Web technologies', 'SAMPLE_QUESTION', 'HTML: structure/content. CSS: styling/presentation. JavaScript: interactivity/behavior. Together they create web pages.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is REST?', 'RESTful API', 'SAMPLE_QUESTION', 'REST is an architectural style using HTTP methods. Resources as URLs. Stateless. JSON/XML format. CRUD: Create-Read-Update-Delete.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('CSS Box Model', 'CSS layout concept', 'SAMPLE_QUESTION', 'Box Model: content (width/height), padding, border, margin. box-sizing: border-box includes padding/border in width.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is JSON?', 'Data format', 'SAMPLE_QUESTION', 'JSON (JavaScript Object Notation) is lightweight data interchange format. Key-value pairs, arrays, supported by most languages. API responses.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('API vs REST API', 'Web services', 'SAMPLE_QUESTION', 'API: interface for software communication. REST API: uses HTTP methods, stateless, JSON. GraphQL: single endpoint, client specifies needed data.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Responsive design', 'Mobile-friendly', 'SAMPLE_QUESTION', 'Responsive design uses fluid grids, flexible images, media queries to adapt layout to screen sizes (mobile, tablet, desktop).', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is CSS Flexbox?', 'Layout system', 'SAMPLE_QUESTION', 'Flexbox is CSS layout module for one-dimensional layouts. Container: display: flex. Properties: justify-content, align-items, flex-direction.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('JavaScript DOM', 'Document Object Model', 'SAMPLE_QUESTION', 'DOM represents HTML as tree of nodes. JavaScript manipulates elements: getElementById, querySelector, addEventListener, innerHTML.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('HTTP Status Codes', 'Response categories', 'SAMPLE_QUESTION', '1xx: Informational. 2xx: Success (200 OK, 201 Created). 3xx: Redirection. 4xx: Client Error (404 Not Found). 5xx: Server Error.', 'WEB_DEVELOPMENT', 1)",
            
            // MS Word (10)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is MS Word?', 'Word processing software', 'SAMPLE_QUESTION', 'MS Word is a word processing application by Microsoft used to create, edit, format, and print documents. Part of Microsoft Office suite.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is formatting in Word?', 'Text formatting', 'SAMPLE_QUESTION', 'Formatting in Word includes changing font type, size, color, bold, italic, underline, alignment, line spacing, and paragraph indentation.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a header and footer?', 'Document sections', 'SAMPLE_QUESTION', 'Header appears at the top of every page, footer at the bottom. Used for page numbers, dates, document titles, author names.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is mail merge?', 'Bulk document creation', 'SAMPLE_QUESTION', 'Mail merge creates multiple personalized documents from a template and data source. Used for letters, envelopes, labels, and emails.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a table in Word?', 'Organizing data', 'SAMPLE_QUESTION', 'A table in Word organizes data in rows and columns. Can be created, formatted, sorted, and used for calculations.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is spell check?', 'Error detection', 'SAMPLE_QUESTION', 'Spell check automatically detects spelling and grammar errors. Red underline for spelling, blue for grammar. Right-click to see suggestions.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is track changes?', 'Collaboration tool', 'SAMPLE_QUESTION', 'Track changes records all edits made to a document. Shows insertions, deletions, and formatting changes. Reviewers can accept or reject changes.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a page break?', 'Page control', 'SAMPLE_QUESTION', 'Page break forces content to start on a new page. Types: manual (Ctrl+Enter), section break (different formatting per section).', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a template?', 'Document blueprint', 'SAMPLE_QUESTION', 'A template is a pre-designed document with formatting, styles, and layout. Saves time by providing a starting point for new documents.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Find and Replace?', 'Text search tool', 'SAMPLE_QUESTION', 'Find locates specific text. Replace finds and substitutes text. Supports wildcards, match case, and whole word options. Ctrl+H for replace.', 'MS_WORD', 1)",
            
            // MS Excel (10)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is MS Excel?', 'Spreadsheet software', 'SAMPLE_QUESTION', 'MS Excel is a spreadsheet application for organizing, analyzing, and visualizing data. Uses rows and columns in worksheets.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a cell reference?', 'Identifying cells', 'SAMPLE_QUESTION', 'Cell reference identifies a cell by column letter and row number (e.g., A1). Types: Relative (A1), Absolute ($A$1), Mixed ($A1 or A$1).', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a formula?', 'Calculations in Excel', 'SAMPLE_QUESTION', 'A formula performs calculations. Starts with = sign. Can use operators (+, -, *, /), cell references, and functions.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a function?', 'Built-in operations', 'SAMPLE_QUESTION', 'A function is a predefined formula. Common: SUM (add), AVERAGE (mean), COUNT (count cells), MAX (largest), MIN (smallest), IF (condition).', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a chart?', 'Data visualization', 'SAMPLE_QUESTION', 'Charts visualize data graphically. Types: Bar, Column, Line, Pie, Scatter. Created from selected data using Insert > Chart.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is sorting?', 'Arranging data', 'SAMPLE_QUESTION', 'Sorting arranges data in ascending (A-Z, 0-9) or descending (Z-A, 9-0) order. Can sort by one or multiple columns.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is filtering?', 'Displaying specific data', 'SAMPLE_QUESTION', 'Filtering shows only rows that meet criteria, hiding others. AutoFilter adds dropdown arrows to column headers. Data > Filter.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is conditional formatting?', 'Visual data formatting', 'SAMPLE_QUESTION', 'Conditional formatting applies formatting based on cell values. Highlights cells, uses color scales, data bars, and icon sets.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a worksheet?', 'Workbook component', 'SAMPLE_QUESTION', 'A worksheet is a single spreadsheet tab in a workbook. Contains cells arranged in rows (numbered) and columns (lettered). Default: Sheet1.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is VLOOKUP?', 'Vertical lookup function', 'SAMPLE_QUESTION', 'VLOOKUP searches for a value in the first column and returns a value from another column. Syntax: VLOOKUP(lookup, table, col, match).', 'MS_EXCEL', 1)",
            
            // MS Access (10)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is MS Access?', 'Database software', 'SAMPLE_QUESTION', 'MS Access is a relational database management system (RDBMS) by Microsoft. Stores data in tables, queries, forms, and reports.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a table?', 'Data storage', 'SAMPLE_QUESTION', 'A table stores data in rows (records) and columns (fields). Each field has a data type (Text, Number, Date, Yes/No).', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a query?', 'Data retrieval', 'SAMPLE_QUESTION', 'A query retrieves, adds, updates, or deletes data. Uses SQL or Query Design view. Select query displays data; Action query modifies data.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a form?', 'Data entry interface', 'SAMPLE_QUESTION', 'A form provides a user-friendly interface for entering, viewing, and editing data. Can be customized with labels, text boxes, buttons.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a report?', 'Data presentation', 'SAMPLE_QUESTION', 'A report presents data in a formatted, printable layout. Can include grouping, sorting, calculations, headers, and footers.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a primary key?', 'Unique identifier', 'SAMPLE_QUESTION', 'A primary key uniquely identifies each record in a table. No duplicates, no null values. Usually an AutoNumber or ID field.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a relationship?', 'Table connections', 'SAMPLE_QUESTION', 'A relationship links tables using common fields. Types: One-to-One, One-to-Many, Many-to-Many. Enforced by referential integrity.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a macro?', 'Automation tool', 'SAMPLE_QUESTION', 'A macro automates tasks without programming. Groups of actions that run when triggered. Used for opening forms, running queries.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is data validation?', 'Input control', 'SAMPLE_QUESTION', 'Data validation ensures data entered meets specific rules. Validation Rule property defines criteria. Validation Text shows error message.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is an index?', 'Search optimization', 'SAMPLE_QUESTION', 'An index speeds up data retrieval in a table. Created on frequently searched or sorted fields. Primary key is automatically indexed.', 'MS_ACCESS', 1)",
            
            // MS PowerPoint (10)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is MS PowerPoint?', 'Presentation software', 'SAMPLE_QUESTION', 'MS PowerPoint is a presentation application for creating slide shows. Used for lectures, business presentations, and conferences.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a slide?', 'Presentation page', 'SAMPLE_QUESTION', 'A slide is a single page in a presentation. Contains text, images, shapes, charts, and other elements arranged on a layout.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a slide transition?', 'Slide change effect', 'SAMPLE_QUESTION', 'Transitions are visual effects when moving between slides. Types: Fade, Push, Wipe, Split, Zoom. Duration and sound can be set.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is animation?', 'Element effects', 'SAMPLE_QUESTION', 'Animation adds movement to slide elements. Types: Entrance, Emphasis, Exit, Motion Paths. Applied to text, images, shapes.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Slide Master?', 'Template control', 'SAMPLE_QUESTION', 'Slide Master controls the overall design of all slides. Changes to master (fonts, colors, layouts) apply to entire presentation.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Presenter View?', 'Speaker display', 'SAMPLE_QUESTION', 'Presenter View shows current slide, next slide, notes, and timer on speaker screen while audience sees only the presentation.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a theme?', 'Design template', 'SAMPLE_QUESTION', 'A theme is a set of coordinated colors, fonts, effects, and slide layouts. Applied from Design tab for consistent presentation appearance.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is SmartArt?', 'Graphic organizer', 'SAMPLE_QUESTION', 'SmartArt creates professional diagrams and graphics. Types: List, Process, Cycle, Hierarchy, Relationship, Matrix, Pyramid.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is slide layout?', 'Content arrangement', 'SAMPLE_QUESTION', 'Slide layout defines the arrangement of placeholders on a slide. Predefined: Title Slide, Title and Content, Two Content, Blank.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is hypermedia in presentations?', 'Interactive content', 'SAMPLE_QUESTION', 'Hypermedia adds links, embedded videos, and audio to slides. Hyperlinks navigate between slides or to external resources.', 'MS_POWERPOINT', 1)",
            
            // MS Publisher (10)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is MS Publisher?', 'Desktop publishing', 'SAMPLE_QUESTION', 'MS Publisher is a desktop publishing application for creating professional publications: brochures, newsletters, flyers, cards.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a publication?', 'Designed document', 'SAMPLE_QUESTION', 'A publication is a designed document created in Publisher. Can be for print, email, or web. Built from templates or scratch.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What are design elements?', 'Page design elements', 'SAMPLE_QUESTION', 'Design elements include text boxes, image placeholders, shapes, tables, and building blocks that make up a publication layout.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a text box?', 'Text container', 'SAMPLE_QUESTION', 'A text box is a container for text in a publication. Text can be formatted, linked between boxes, and wrapped around images.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a template?', 'Pre-designed layout', 'SAMPLE_QUESTION', 'Templates provide pre-designed layouts with placeholder text and images. Categories: Brochures, Newsletters, Flyers, Business Cards.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is mail merge in Publisher?', 'Bulk publishing', 'SAMPLE_QUESTION', 'Mail merge in Publisher personalizes publications using data from a list. Creates multiple copies with different names, addresses.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What are building blocks?', 'Reusable design pieces', 'SAMPLE_QUESTION', 'Building blocks are reusable design elements: page parts, borders, calendars, advertisements. Stored in Building Block Library.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a color scheme?', 'Publication colors', 'SAMPLE_QUESTION', 'A color scheme is a set of coordinated colors applied to a publication. Primary, secondary, accent, and background colors defined.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Pack and Go?', 'Publication packaging', 'SAMPLE_QUESTION', 'Pack and Go packages a publication with linked files for commercial printing or for taking to another computer. Creates compressed file.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a layout guide?', 'Page alignment', 'SAMPLE_QUESTION', 'Layout guides define margins, columns, and grid lines for consistent placement of objects. Found in Page Design > Layout Guides.', 'MS_PUBLISHER', 1)",

            // Office Applications (10)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Microsoft Office?', 'Productivity suite', 'SAMPLE_QUESTION', 'Microsoft Office is a suite of productivity applications including Word, Excel, PowerPoint, Access, and Publisher. Used for document creation, data analysis, presentations, and database management.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is the Ribbon in Office?', 'Office interface element', 'SAMPLE_QUESTION', 'The Ribbon is the toolbar interface in Office apps organized into tabs (Home, Insert, Layout) with groups of commands. Replaces traditional menus. Customizable via Quick Access Toolbar.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Save As in Office?', 'File saving options', 'SAMPLE_QUESTION', 'Save As creates a new copy with different name/location/format. Options: local drive, OneDrive cloud. File formats: .docx, .xlsx, .pptx, PDF. AutoSave keeps changes synced in cloud.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Copy and Paste in Office?', 'Content transfer', 'SAMPLE_QUESTION', 'Copy (Ctrl+C) duplicates selected content. Paste (Ctrl+V) inserts it. Paste Special offers format options: Keep Source Formatting, Merge Formatting, Keep Text Only.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Undo and Redo?', 'Action reversal', 'SAMPLE_QUESTION', 'Undo (Ctrl+Z) reverses the last action. Redo (Ctrl+Y) restores undone action. Office keeps history of recent actions. Useful for correcting mistakes quickly.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Find and Replace?', 'Text search tool', 'SAMPLE_QUESTION', 'Find (Ctrl+F) locates specific text. Replace (Ctrl+H) finds and substitutes text. Options: match case, whole word, wildcards. Available across all Office apps.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a Template in Office?', 'Pre-designed document', 'SAMPLE_QUESTION', 'A template is a pre-designed document with formatting, styles, and layout. Saves time by providing starting point. Categories: resumes, reports, budgets, presentations.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Format Painter?', 'Style copying tool', 'SAMPLE_QUESTION', 'Format Painter copies formatting from one element and applies it to another. Single-click copies once, double-click copies multiple times. Found in Home tab Clipboard group.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Spell Check in Office?', 'Error detection', 'SAMPLE_QUESTION', 'Spell check automatically detects spelling (red underline) and grammar (blue underline) errors. AutoCorrect fixes common mistakes. Review tab provides full document check.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Office 365?', 'Cloud-based Office', 'SAMPLE_QUESTION', 'Office 365 (now Microsoft 365) is a subscription-based cloud service providing Office apps, OneDrive storage, and collaboration tools. Features: co-authoring, cloud storage, always up-to-date.', 'OFFICE_APPLICATIONS', 1)",

            // Internet and Email (10)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is the Internet?', 'Global network', 'SAMPLE_QUESTION', 'The Internet is a global network of interconnected computers using TCP/IP protocol. Provides access to websites, email, file sharing, and services.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a web browser?', 'Internet access tool', 'SAMPLE_QUESTION', 'A web browser is software to access websites. Examples: Chrome, Firefox, Edge, Safari. Renders HTML, CSS, JavaScript.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a URL?', 'Web address', 'SAMPLE_QUESTION', 'URL (Uniform Resource Locator) is a web address. Parts: protocol (https://), domain (www.example.com), path (/page).', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is email?', 'Electronic mail', 'SAMPLE_QUESTION', 'Email (electronic mail) sends messages electronically. Requires email address (user@domain.com). Features: attachments, CC, BCC, forwarding.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a search engine?', 'Web search tool', 'SAMPLE_QUESTION', 'A search engine finds information on the web. Examples: Google, Bing, Yahoo. Uses crawlers, indexing, and ranking algorithms.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is a domain name?', 'Website identifier', 'SAMPLE_QUESTION', 'A domain name is a human-readable website address. Parts: subdomain (www), domain (google), extension (.com). Translated to IP by DNS.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is phishing?', 'Internet fraud', 'SAMPLE_QUESTION', 'Phishing is a cyber attack using fake emails or websites to steal personal information. Protection: verify sender, do not click suspicious links.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Wi-Fi?', 'Wireless networking', 'SAMPLE_QUESTION', 'Wi-Fi is wireless networking technology using radio waves. IEEE 802.11 standard. Connects devices to router/network without cables.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is cloud computing?', 'Online services', 'SAMPLE_QUESTION', 'Cloud computing delivers services over the Internet: storage (Google Drive), computing (AWS), software (Office 365). Pay-as-you-go model.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is netiquette?', 'Online etiquette', 'SAMPLE_QUESTION', 'Netiquette is online etiquette. Rules: be polite, do not SHOUT (ALL CAPS), respect privacy, avoid spam, cite sources.', 'INTERNET_EMAIL', 1)",
            
            // Emerging Trends (10)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Artificial Intelligence?', 'Machine intelligence', 'SAMPLE_QUESTION', 'AI simulates human intelligence in machines. Areas: Machine Learning, Natural Language Processing, Computer Vision, Robotics.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is Machine Learning?', 'Data-driven learning', 'SAMPLE_QUESTION', 'ML enables computers to learn from data without explicit programming. Types: Supervised (labeled data), Unsupervised (patterns), Reinforcement.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is IoT?', 'Internet of Things', 'SAMPLE_QUESTION', 'IoT connects everyday devices to the Internet for data exchange. Examples: smart home devices, wearables, industrial sensors.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is blockchain?', 'Distributed ledger', 'SAMPLE_QUESTION', 'Blockchain is a decentralized, distributed ledger technology. Blocks linked cryptographically. Used in cryptocurrency, supply chain, smart contracts.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is big data?', 'Large data sets', 'SAMPLE_QUESTION', 'Big data refers to extremely large data sets. Characteristics: Volume (size), Velocity (speed), Variety (types), Veracity (quality), Value.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is cybersecurity?', 'Digital protection', 'SAMPLE_QUESTION', 'Cybersecurity protects systems and data from attacks. Threats: malware, phishing, ransomware. Measures: firewalls, encryption, antivirus.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is cloud computing?', 'On-demand computing', 'SAMPLE_QUESTION', 'Cloud computing delivers computing services over the Internet. Models: IaaS, PaaS, SaaS. Providers: AWS, Azure, Google Cloud.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is virtual reality?', 'Immersive technology', 'SAMPLE_QUESTION', 'VR creates computer-generated immersive environments. Used in gaming, training, education. Requires headset (Oculus, HTC Vive).', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is 5G?', 'Next-gen connectivity', 'SAMPLE_QUESTION', '5G is fifth-generation mobile network technology. Faster speeds, lower latency, more device connections than 4G. Enables IoT and autonomous vehicles.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('What is quantum computing?', 'Advanced computing', 'SAMPLE_QUESTION', 'Quantum computing uses quantum mechanics (qubits) for computation. Can solve complex problems faster than classical computers. Applications: cryptography, drug discovery.', 'EMERGING_TRENDS', 1)"
        };
        
        // Insert sample exams - Expanded
        String[] exams = {
            // Programming (2 exams)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Programming Basics Exam', 'Test programming fundamentals', 'EXAM', 'Q1: What is a variable? Answer: A container for storing data. Q2: Which data type for whole numbers? Answer: int. Q3: Keyword to define class? Answer: class. Q4: What is a loop? Answer: Repeats code execution. Q5: What is if-else? Answer: Conditional statement.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('OOP Principles Exam', 'Object-oriented programming test', 'EXAM', 'Q1: Four pillars of OOP? Answer: Encapsulation, Inheritance, Polymorphism, Abstraction. Q2: What is inheritance? Answer: Child class inherits from parent. Q3: What is encapsulation? Answer: Data hiding. Q4: Interface vs abstract class? Answer: Interface only abstract methods, abstract has both. Q5: What is polymorphism? Answer: Multiple forms.', 'PROGRAMMING', 1)",
            
            // Data Structures (2 exams)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Data Structures Quiz', 'Data structures test', 'EXAM', 'Q1: Time complexity of array access? Answer: O(1). Q2: Which uses FIFO? Answer: Queue. Q3: What is linked list? Answer: Collection of nodes with pointers. Q4: BST property? Answer: Left < root < right. Q5: Hash table avg lookup? Answer: O(1).', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Advanced Data Structures Exam', 'Complex data structures test', 'EXAM', 'Q1: AVL tree balance factor? Answer: Height difference <= 1. Q2: Heap type for max? Answer: Max-Heap. Q3: BFS uses which data structure? Answer: Queue. Q4: Graph representations? Answer: Adjacency list, matrix. Q5: Trie used for? Answer: Autocomplete, prefix matching.', 'DATA_STRUCTURES', 1)",
            
            // Algorithms (2 exams)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Algorithms Final Exam', 'Comprehensive algorithms test', 'EXAM', 'Q1: Bubble sort complexity? Answer: O(n^2). Q2: Uses divide and conquer? Answer: Quick Sort. Q3: Best case quick sort? Answer: O(n log n). Q4: Dijkstra time complexity? Answer: O((V+E) log V). Q5: Binary search complexity? Answer: O(log n).', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Algorithm Design Techniques', 'Paradigms and approaches', 'EXAM', 'Q1: Greedy approach example? Answer: Huffman coding. Q2: DP solves what? Answer: Overlapping subproblems. Q3: DFS uses? Answer: Stack or recursion. Q4: Merge sort complexity? Answer: O(n log n). Q5: What is backtracking? Answer: Trial and error with pruning.', 'ALGORITHMS', 1)",
            
            // Databases (2 exams)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Database Systems Test', 'SQL and database concepts', 'EXAM', 'Q1: Which command retrieves data? Answer: SELECT. Q2: PRIMARY KEY does? Answer: Uniquely identifies each record. Q3: What is foreign key? Answer: References another table. Q4: SQL JOIN types? Answer: INNER, LEFT, RIGHT, FULL. Q5: Normalization purpose? Answer: Reduce redundancy.', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Advanced SQL Exam', 'Complex queries test', 'EXAM', 'Q1: GROUP BY purpose? Answer: Group rows for aggregates. Q2: HAVING vs WHERE? Answer: HAVING filters groups, WHERE filters rows. Q3: Index purpose? Answer: Speed up retrieval. Q4: ACID properties? Answer: Atomicity, Consistency, Isolation, Durability. Q5: NoSQL types? Answer: Document, Key-Value, Column, Graph.', 'DATABASES', 1)",
            
            // Web Development (2 exams)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Web Development Fundamentals', 'HTML, CSS, JavaScript basics', 'EXAM', 'Q1: HTML stands for? Answer: HyperText Markup Language. Q2: CSS purpose? Answer: Styling. Q3: JavaScript runs where? Answer: Client-side (browser). Q4: GET vs POST? Answer: GET retrieves, POST creates. Q5: What is responsive design? Answer: Adapts to screen size.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Modern Web Technologies', 'APIs and frameworks', 'EXAM', 'Q1: REST principles? Answer: Stateless, resource-based, HTTP methods. Q2: JSON format? Answer: JavaScript Object Notation. Q3: Flexbox property for horizontal? Answer: justify-content. Q4: What is an API? Answer: Application Programming Interface. Q5: DOM stands for? Answer: Document Object Model.', 'WEB_DEVELOPMENT', 1)",
            
            // MS Word (2 exams)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('MS Word Basics Exam', 'Word processing fundamentals', 'EXAM', 'Q1: What is MS Word? Answer: Word processing software. Q2: What shortcut for bold? Answer: Ctrl+B. Q3: What is mail merge? Answer: Creates personalized bulk documents. Q4: What is track changes? Answer: Records edits for review. Q5: What is a template? Answer: Pre-designed document blueprint.', 'MS_WORD', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('MS Word Advanced Exam', 'Advanced word processing', 'EXAM', 'Q1: What is a section break? Answer: Divides document for different formatting. Q2: What is a table of contents? Answer: Auto-generated list of headings with page numbers. Q3: What is find and replace? Answer: Searches and substitutes text. Q4: What is a header/footer? Answer: Top/bottom page content repeated on each page. Q5: What is spell check? Answer: Detects spelling and grammar errors.', 'MS_WORD', 1)",
            
            // MS Excel (2 exams)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('MS Excel Basics Exam', 'Spreadsheet fundamentals', 'EXAM', 'Q1: What is a cell? Answer: Intersection of row and column. Q2: Formula starts with? Answer: = sign. Q3: SUM function purpose? Answer: Adds values. Q4: What is a chart? Answer: Visual representation of data. Q5: What is sorting? Answer: Arranging data in order.', 'MS_EXCEL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('MS Excel Functions Exam', 'Excel functions and formulas', 'EXAM', 'Q1: VLOOKUP purpose? Answer: Vertical lookup in a table. Q2: What is absolute reference? Answer: Fixed cell reference ($A$1). Q3: What is conditional formatting? Answer: Formatting based on cell values. Q4: What is filtering? Answer: Showing only matching rows. Q5: COUNT vs COUNTA? Answer: COUNT counts numbers, COUNTA counts non-empty cells.', 'MS_EXCEL', 1)",
            
            // MS Access (2 exams)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('MS Access Basics Exam', 'Database fundamentals', 'EXAM', 'Q1: What is MS Access? Answer: Relational database management system. Q2: What is a table? Answer: Stores data in rows and columns. Q3: What is a query? Answer: Retrieves or modifies data. Q4: What is a form? Answer: User interface for data entry. Q5: What is a report? Answer: Formatted printable data output.', 'MS_ACCESS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('MS Access Relationships Exam', 'Database relationships and queries', 'EXAM', 'Q1: What is a primary key? Answer: Unique record identifier. Q2: What is a foreign key? Answer: References primary key in another table. Q3: Relationship types? Answer: One-to-One, One-to-Many, Many-to-Many. Q4: What is referential integrity? Answer: Ensures related data consistency. Q5: What is a macro? Answer: Automated task without programming.', 'MS_ACCESS', 1)",
            
            // MS PowerPoint (2 exams)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('MS PowerPoint Basics Exam', 'Presentation fundamentals', 'EXAM', 'Q1: What is PowerPoint? Answer: Presentation software. Q2: What is a slide? Answer: Single page in presentation. Q3: What is a transition? Answer: Visual effect between slides. Q4: What is animation? Answer: Movement effect on elements. Q5: What is a theme? Answer: Coordinated design template.', 'MS_POWERPOINT', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('MS PowerPoint Advanced Exam', 'Advanced presentation features', 'EXAM', 'Q1: What is Slide Master? Answer: Controls design of all slides. Q2: What is Presenter View? Answer: Speaker-only display with notes. Q3: What is SmartArt? Answer: Pre-built professional diagrams. Q4: What is hypermedia? Answer: Links and embedded media. Q5: Slide layout purpose? Answer: Arranges placeholders on a slide.', 'MS_POWERPOINT', 1)",
            
            // MS Publisher (2 exams)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('MS Publisher Basics Exam', 'Desktop publishing fundamentals', 'EXAM', 'Q1: What is Publisher? Answer: Desktop publishing application. Q2: What is a publication? Answer: Designed document for print or web. Q3: What is a text box? Answer: Container for text in publication. Q4: What is a template? Answer: Pre-designed publication layout. Q5: What is a building block? Answer: Reusable design element.', 'MS_PUBLISHER', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('MS Publisher Design Exam', 'Publication design features', 'EXAM', 'Q1: What is mail merge in Publisher? Answer: Personalizes bulk publications. Q2: What is a color scheme? Answer: Set of coordinated publication colors. Q3: What is Pack and Go? Answer: Packages publication for printing/computer transfer. Q4: What is a layout guide? Answer: Defines margins and grid lines. Q5: What are design elements? Answer: Text boxes, images, shapes, tables.', 'MS_PUBLISHER', 1)",

            // Office Applications (2 exams)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Office Applications Fundamentals Exam', 'Microsoft Office suite basics', 'EXAM', 'Q1: What is Microsoft Office? Answer: Suite of productivity applications. Q2: What is the Ribbon? Answer: Tabbed toolbar interface. Q3: What is a template? Answer: Pre-designed document layout. Q4: What is Format Painter? Answer: Copies formatting to other elements. Q5: What is Office 365? Answer: Cloud-based subscription Office service.', 'OFFICE_APPLICATIONS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Office Productivity Skills Exam', 'Office efficiency and tools', 'EXAM', 'Q1: What does Ctrl+Z do? Answer: Undo last action. Q2: What is Find and Replace? Answer: Locates and substitutes text. Q3: What is Spell Check? Answer: Detects spelling and grammar errors. Q4: What is Paste Special? Answer: Paste with format options. Q5: What is AutoSave? Answer: Automatic saving in cloud storage.', 'OFFICE_APPLICATIONS', 1)",

            // Internet and Email (2 exams)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Internet Basics Exam', 'Internet fundamentals', 'EXAM', 'Q1: What is the Internet? Answer: Global network of computers. Q2: What is a web browser? Answer: Software to access websites. Q3: What is a URL? Answer: Uniform Resource Locator (web address). Q4: What is a search engine? Answer: Tool to find web information. Q5: What is a domain name? Answer: Human-readable website address.', 'INTERNET_EMAIL', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Email and Security Exam', 'Email usage and online safety', 'EXAM', 'Q1: What is email? Answer: Electronic mail for sending messages. Q2: What is phishing? Answer: Fraud to steal personal information. Q3: What is Wi-Fi? Answer: Wireless networking technology. Q4: What is cloud computing? Answer: Services delivered over the Internet. Q5: What is netiquette? Answer: Online etiquette rules.', 'INTERNET_EMAIL', 1)",
            
            // Emerging Trends (2 exams)
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Emerging Technologies Exam', 'Modern technology trends', 'EXAM', 'Q1: What is AI? Answer: Simulated human intelligence in machines. Q2: What is IoT? Answer: Internet of Things - connected devices. Q3: What is blockchain? Answer: Decentralized distributed ledger. Q4: What is big data? Answer: Extremely large data sets. Q5: What is 5G? Answer: Fifth-generation mobile network.', 'EMERGING_TRENDS', 1)",
            "INSERT INTO content (title, description, type, body, category, created_by) VALUES ('Future of Technology Exam', 'Advanced emerging concepts', 'EXAM', 'Q1: What is Machine Learning? Answer: Computers learning from data. Q2: What is cybersecurity? Answer: Protection from digital attacks. Q3: What is virtual reality? Answer: Computer-generated immersive environment. Q4: What is quantum computing? Answer: Computing using quantum mechanics. Q5: Cloud computing models? Answer: IaaS, PaaS, SaaS.', 'EMERGING_TRENDS', 1)"
        };
        
        // Insert video content
        String[] videos = {
            "INSERT INTO content (title, description, type, content_url, body, category, created_by) VALUES ('Introduction to Programming Video', 'Video lesson on programming basics', 'VIDEO', 'https://www.youtube.com/watch?v=zOjov-2OZ0E', 'Watch this comprehensive introduction to programming concepts including variables, loops, and functions.', 'PROGRAMMING', 1)",
            "INSERT INTO content (title, description, type, content_url, body, category, created_by) VALUES ('Data Structures Visualized', 'Visual guide to data structures', 'VIDEO', 'https://www.youtube.com/watch?v=9rhT3P1mD_4', 'Learn about arrays, linked lists, trees, and graphs with visual explanations.', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, content_url, body, category, created_by) VALUES ('Sorting Algorithms Explained', 'Visual sorting algorithm guide', 'VIDEO', 'https://www.youtube.com/watch?v=kgBjXUE_Nzs', 'Watch different sorting algorithms in action: Bubble, Selection, Insertion, Quick, Merge.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, content_url, body, category, created_by) VALUES ('SQL Tutorial for Beginners', 'Learn SQL from scratch', 'VIDEO', 'https://www.youtube.com/watch?v=HXV3zeQKqTY', 'Complete SQL tutorial covering SELECT, JOIN, GROUP BY, and more.', 'DATABASES', 1)",
            "INSERT INTO content (title, description, type, content_url, body, category, created_by) VALUES ('HTML/CSS Full Course', 'Web development basics', 'VIDEO', 'https://www.youtube.com/watch?v=yfoY53QXEnI', 'Learn HTML and CSS from scratch to build beautiful websites.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, content_url, body, category, created_by) VALUES ('JavaScript Crash Course', 'JavaScript fundamentals', 'VIDEO', 'https://www.youtube.com/watch?v=W6NZfCO5SIk', 'Master JavaScript basics in this comprehensive tutorial.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, content_url, body, category, created_by) VALUES ('Big O Notation Explained', 'Algorithm complexity made easy', 'VIDEO', 'https://www.youtube.com/watch?v=Mo4vesaut8g', 'Understand Big O notation and algorithm complexity analysis.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, content_url, body, category, created_by) VALUES ('React JS Tutorial', 'Learn React from scratch', 'VIDEO', 'https://www.youtube.com/watch?v=Tn6-PIqc4UM', 'Build modern web applications with React.js.', 'WEB_DEVELOPMENT', 1)",
            "INSERT INTO content (title, description, type, content_url, body, category, created_by) VALUES ('Binary Trees and BST', 'Tree data structures', 'VIDEO', 'https://www.youtube.com/watch?v=H5Jub-5cNA8', 'Learn about binary trees, BST operations, and tree traversals.', 'DATA_STRUCTURES', 1)",
            "INSERT INTO content (title, description, type, content_url, body, category, created_by) VALUES ('Graph Algorithms', 'BFS and DFS explained', 'VIDEO', 'https://www.youtube.com/watch?v=pcKY4hjDrxk', 'Master graph traversal algorithms with practical examples.', 'ALGORITHMS', 1)",
            "INSERT INTO content (title, description, type, content_url, body, category, created_by) VALUES ('Microsoft Office Complete Tutorial', 'Full Office suite guide', 'VIDEO', 'https://www.youtube.com/watch?v=KE4FJxkYOTM', 'Learn Microsoft Office applications including Word, Excel, PowerPoint, and more in this comprehensive tutorial.', 'OFFICE_APPLICATIONS', 1)"
        };
        
        try (Statement stmt = connection.createStatement()) {
            for (String sql : notes) {
                stmt.execute(sql);
            }
            for (String sql : questions) {
                stmt.execute(sql);
            }
            for (String sql : exams) {
                stmt.execute(sql);
            }
            for (String sql : videos) {
                stmt.execute(sql);
            }
        }
        
        System.out.println("Sample content inserted successfully: 120 notes, 120 questions, 24 exams, 10 videos");
    }

    // Project operations
    public static List<Map<String, Object>> getAllProjects() throws SQLException {
        List<Map<String, Object>> projects = new ArrayList<>();
        String sql = "SELECT * FROM projects ORDER BY created_at DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> project = new HashMap<>();
                project.put("id", rs.getInt("id"));
                project.put("title", rs.getString("title"));
                project.put("description", rs.getString("description"));
                project.put("project_url", rs.getString("project_url"));
                project.put("status", rs.getString("status"));
                project.put("progress", rs.getInt("progress"));
                project.put("technologies", rs.getString("technologies"));
                project.put("cover_color", rs.getString("cover_color"));
                project.put("icon", rs.getString("icon"));
                project.put("created_by", rs.getInt("created_by"));
                project.put("created_at", rs.getTimestamp("created_at"));
                projects.add(project);
            }
        }
        return projects;
    }

    public static Map<String, Object> getProjectById(int id) throws SQLException {
        String sql = "SELECT * FROM projects WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> project = new HashMap<>();
                project.put("id", rs.getInt("id"));
                project.put("title", rs.getString("title"));
                project.put("description", rs.getString("description"));
                project.put("project_url", rs.getString("project_url"));
                project.put("status", rs.getString("status"));
                project.put("progress", rs.getInt("progress"));
                project.put("technologies", rs.getString("technologies"));
                project.put("cover_color", rs.getString("cover_color"));
                project.put("icon", rs.getString("icon"));
                project.put("created_by", rs.getInt("created_by"));
                project.put("created_at", rs.getTimestamp("created_at"));
                return project;
            }
        }
        return null;
    }

    public static int addProject(String title, String description, String projectUrl, String status, int progress, String technologies, String coverColor, String icon, int createdBy) throws SQLException {
        String sql = "INSERT INTO projects (title, description, project_url, status, progress, technologies, cover_color, icon, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, title);
            pstmt.setString(2, description);
            pstmt.setString(3, projectUrl);
            pstmt.setString(4, status);
            pstmt.setInt(5, progress);
            pstmt.setString(6, technologies);
            pstmt.setString(7, coverColor);
            pstmt.setString(8, icon);
            pstmt.setInt(9, createdBy);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    public static void updateProject(int id, String title, String description, String projectUrl, String status, int progress, String technologies, String coverColor, String icon) throws SQLException {
        String sql = "UPDATE projects SET title = ?, description = ?, project_url = ?, status = ?, progress = ?, technologies = ?, cover_color = ?, icon = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, description);
            pstmt.setString(3, projectUrl);
            pstmt.setString(4, status);
            pstmt.setInt(5, progress);
            pstmt.setString(6, technologies);
            pstmt.setString(7, coverColor);
            pstmt.setString(8, icon);
            pstmt.setInt(9, id);
            pstmt.executeUpdate();
        }
    }

    public static void deleteProject(int id) throws SQLException {
        String sql = "DELETE FROM projects WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public static void insertSampleProjects() throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM projects";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        addProject(
            "Personal Portfolio",
            "A responsive portfolio website showcasing my projects, skills, and experience. Built with modern HTML, CSS, and JavaScript featuring dark mode and smooth animations.",
            "https://timokama.github.io",
            "Completed",
            100,
            "HTML,CSS,JavaScript",
            "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
            "\uD83C\uDFA8",
            1
        );

        addProject(
            "Ultimate Defensive Driving",
            "An interactive web application for defensive driving education and training. Features course modules, quizzes, and progress tracking for learners.",
            "https://ultimate-defensive-driving.onrender.com",
            "In Progress",
            75,
            "HTML,CSS,JavaScript,Node.js",
            "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)",
            "\uD83D\uDE97",
            1
        );

        System.out.println("Sample projects inserted successfully");
    }

    public static void deleteUser(int userId) throws SQLException {
        // Delete related subscriptions first
        String deleteSubs = "DELETE FROM subscriptions WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSubs)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
        // Delete the user
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }
    
    public static void deleteContent(int contentId) throws SQLException {
        String sql = "DELETE FROM content WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, contentId);
            pstmt.executeUpdate();
        }
    }
    
    public static void updateUser(int userId, String fullName, String role, boolean active) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, role = ?, active = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, fullName);
            pstmt.setString(2, role);
            pstmt.setBoolean(3, active);
            pstmt.setInt(4, userId);
            pstmt.executeUpdate();
        }
    }
    
    public static void updateContent(int contentId, String title, String type, String category) throws SQLException {
        String sql = "UPDATE content SET title = ?, type = ?, category = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, type);
            pstmt.setString(3, category);
            pstmt.setInt(4, contentId);
            pstmt.executeUpdate();
        }
    }

    public static void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Failed to close database connection: " + e.getMessage());
        }
    }
}