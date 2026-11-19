import java.sql.*;
import java.util.Scanner;

/**
 * Student Performance System - JDBC + MySQL
 * Run student_performance.sql first to create the schema and sample data.
 */
public class StudentPerformanceSystem {

    // ---- change these to match your MySQL setup ----
    private static final String URL  = "jdbc:mysql://localhost:3306/student_db";
    private static final String USER = "root";
    private static final String PASS = "password";

    private static final String AVG_EXPR =
            "(sc.programming + sc.mathematics + sc.dbms) / 3";

    private static final String GREATEST_EXPR =
            "GREATEST(sc.programming, sc.mathematics, sc.dbms)";

    // ------------------------------------------------------------------
    // Main menu
    // ------------------------------------------------------------------
    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             Scanner in = new Scanner(System.in)) {

            System.out.println("Connected to student_db");
            boolean running = true;
            while (running) {
                printMenu();
                int choice = readInt(in, "Enter choice: ");
                try {
                    switch (choice) {
                        case 1:  addStudent(con, in);          break;
                        case 2:  addSkill(con, in);            break;
                        case 3:  showAveragesAndCategory(con); break;
                        case 4:  showStrongerSubject(con);     break;
                        case 5:  searchStudent(con, in);       break;
                        case 6:  meritList(con);               break;
                        case 7:  sameSkills(con);              break;
                        case 8:  needsImprovement(con);        break;
                        case 0:  running = false;              break;
                        default: System.out.println("Invalid choice.");
                    }
                } catch (SQLException e) {
                    handleSQLException(e);
                }
            }
            System.out.println("Goodbye!");
        } catch (SQLException e) {
            System.out.println("Could not connect: " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("\n===== STUDENT PERFORMANCE SYSTEM =====");
        System.out.println(" 1. Add student with scores");
        System.out.println(" 2. Add skill to a student");
        System.out.println(" 3. Average score and performance category");
        System.out.println(" 4. Stronger subject of each student");
        System.out.println(" 5. Search by ID / email / course");
        System.out.println(" 6. Merit list");
        System.out.println(" 7. Students with the same technical skills");
        System.out.println(" 8. Weak in programming but maths >= 75");
        System.out.println(" 0. Exit");
    }

    // ------------------------------------------------------------------
    // Req 8: duplicate ID prevention + score validation
    // ------------------------------------------------------------------
    private static void addStudent(Connection con, Scanner in) throws SQLException {
        int id = readInt(in, "Student ID: ");
        System.out.print("Name: ");   String name   = in.nextLine().trim();
        System.out.print("Email: ");  String email  = in.nextLine().trim();
        System.out.print("Course: "); String course = in.nextLine().trim();

        double prog = readScore(in, "Programming score (0-100): ");
        double math = readScore(in, "Mathematics score (0-100): ");
        double db   = readScore(in, "Databases score (0-100): ");

        // Application-level check for a friendlier message than the DB error
        try (PreparedStatement chk = con.prepareStatement(
                "SELECT 1 FROM students WHERE student_id = ?")) {
            chk.setInt(1, id);
            try (ResultSet rs = chk.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Student ID " + id + " already exists. Not added.");
                    return;
                }
            }
        }

        String insStudent = "INSERT INTO students (student_id, name, email, course) VALUES (?,?,?,?)";
        String insScores  = "INSERT INTO scores (student_id, programming, mathematics, dbms) VALUES (?,?,?,?)";

        con.setAutoCommit(false);       // both inserts succeed or neither does
        try (PreparedStatement ps1 = con.prepareStatement(insStudent);
             PreparedStatement ps2 = con.prepareStatement(insScores)) {

            ps1.setInt(1, id);
            ps1.setString(2, name);
            ps1.setString(3, email);
            ps1.setString(4, course);
            ps1.executeUpdate();

            ps2.setInt(1, id);
            ps2.setDouble(2, prog);
            ps2.setDouble(3, math);
            ps2.setDouble(4, db);
            ps2.executeUpdate();

            con.commit();
            System.out.println("Student added successfully.");
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private static void addSkill(Connection con, Scanner in) throws SQLException {
        int id = readInt(in, "Student ID: ");
        System.out.print("Skill name: ");
        String skill = in.nextLine().trim();

        try (PreparedStatement p = con.prepareStatement(
                "INSERT IGNORE INTO skills (skill_name) VALUES (?)")) {
            p.setString(1, skill);
            p.executeUpdate();
        }

        int skillId;
        try (PreparedStatement p = con.prepareStatement(
                "SELECT skill_id FROM skills WHERE skill_name = ?")) {
            p.setString(1, skill);
            try (ResultSet rs = p.executeQuery()) {
                rs.next();
                skillId = rs.getInt(1);
            }
        }

        try (PreparedStatement p = con.prepareStatement(
                "INSERT INTO student_skills (student_id, skill_id) VALUES (?,?)")) {
            p.setInt(1, id);
            p.setInt(2, skillId);
            p.executeUpdate();
            System.out.println("Skill linked to student.");
        }
    }

    // ------------------------------------------------------------------
    // Req 1 + 2: average and category
    // ------------------------------------------------------------------
    private static void showAveragesAndCategory(Connection con) throws SQLException {
        String sql =
            "SELECT s.student_id, s.name, " +
            "       ROUND(" + AVG_EXPR + ", 2) AS average_score, " +
            "       CASE " +
            "         WHEN " + AVG_EXPR + " >= 85 THEN 'Excellent' " +
            "         WHEN " + AVG_EXPR + " >= 70 THEN 'Good' " +
            "         WHEN " + AVG_EXPR + " >= 50 THEN 'Satisfactory' " +
            "         ELSE 'Needs Improvement' END AS category " +
            "FROM students s JOIN scores sc ON sc.student_id = s.student_id";
        runQuery(con, sql);
    }

    // ------------------------------------------------------------------
    // Req 3: stronger subject (ties shown together)
    // ------------------------------------------------------------------
    private static void showStrongerSubject(Connection con) throws SQLException {
        String sql =
            "SELECT s.student_id, s.name, " + GREATEST_EXPR + " AS highest_score, " +
            "  CONCAT_WS(', ', " +
            "    IF(sc.programming = " + GREATEST_EXPR + ", 'Programming', NULL), " +
            "    IF(sc.mathematics = " + GREATEST_EXPR + ", 'Mathematics', NULL), " +
            "    IF(sc.dbms   = " + GREATEST_EXPR + ", 'Databases',   NULL)) AS stronger_subject " +
            "FROM students s JOIN scores sc ON sc.student_id = s.student_id";
        runQuery(con, sql);
    }

    // ------------------------------------------------------------------
    // Req 4: search by ID, email or course
    // ------------------------------------------------------------------
    private static void searchStudent(Connection con, Scanner in) throws SQLException {
        System.out.print("Enter student ID, email or course: ");
        String term = in.nextLine().trim();

        String sql =
            "SELECT student_id, name, email, course FROM students " +
            "WHERE CAST(student_id AS CHAR) = ? OR email = ? OR course LIKE ?";
        runQuery(con, sql, term, term, "%" + term + "%");
    }

    // ------------------------------------------------------------------
    // Req 5: merit list
    // ------------------------------------------------------------------
    private static void meritList(Connection con) throws SQLException {
        String sql =
            "SELECT RANK() OVER (ORDER BY " + AVG_EXPR + " DESC) AS merit_rank, " +
            "       s.student_id, s.name, s.course, " +
            "       ROUND(" + AVG_EXPR + ", 2) AS average_score " +
            "FROM students s JOIN scores sc ON sc.student_id = s.student_id " +
            "ORDER BY average_score DESC";
        runQuery(con, sql);
    }

    // ------------------------------------------------------------------
    // Req 6: same technical skills
    // ------------------------------------------------------------------
    private static void sameSkills(Connection con) throws SQLException {
        System.out.println("\n-- Students with an identical set of skills --");
        String exact =
            "SELECT skill_set, COUNT(*) AS student_count, " +
            "       GROUP_CONCAT(name ORDER BY name SEPARATOR ', ') AS students " +
            "FROM ( " +
            "  SELECT s.student_id, s.name, " +
            "         GROUP_CONCAT(k.skill_name ORDER BY k.skill_name) AS skill_set " +
            "  FROM students s " +
            "  JOIN student_skills ss ON ss.student_id = s.student_id " +
            "  JOIN skills k ON k.skill_id = ss.skill_id " +
            "  GROUP BY s.student_id, s.name) t " +
            "GROUP BY skill_set HAVING COUNT(*) > 1";
        runQuery(con, exact);

        System.out.println("\n-- Pairs sharing at least one skill --");
        String pairs =
            "SELECT a.student_id AS student1, b.student_id AS student2, k.skill_name " +
            "FROM student_skills a " +
            "JOIN student_skills b ON a.skill_id = b.skill_id AND a.student_id < b.student_id " +
            "JOIN skills k ON k.skill_id = a.skill_id " +
            "ORDER BY k.skill_name, student1, student2";
        runQuery(con, pairs);
    }

    // ------------------------------------------------------------------
    // Req 7: needs improvement in programming but strong in maths
    // ------------------------------------------------------------------
    private static void needsImprovement(Connection con) throws SQLException {
        String sql =
            "SELECT s.student_id, s.name, sc.programming, sc.mathematics " +
            "FROM students s JOIN scores sc ON sc.student_id = s.student_id " +
            "WHERE sc.programming < 50 AND sc.mathematics >= 75";
        runQuery(con, sql);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Runs a SELECT with optional parameters and prints it as a table. */
    private static void runQuery(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                printResultSet(rs);
            }
        }
    }

    private static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();

        StringBuilder header = new StringBuilder();
        for (int i = 1; i <= cols; i++) {
            header.append(String.format("%-24s", md.getColumnLabel(i)));
        }
        System.out.println(header);
        System.out.println("-".repeat(header.length()));

        int rows = 0;
        while (rs.next()) {
            StringBuilder line = new StringBuilder();
            for (int i = 1; i <= cols; i++) {
                line.append(String.format("%-24s", rs.getString(i)));
            }
            System.out.println(line);
            rows++;
        }
        if (rows == 0) System.out.println("(no records found)");
    }

    private static void handleSQLException(SQLException e) {
        switch (e.getErrorCode()) {
            case 1062: System.out.println("Error: duplicate entry (student ID or email already exists)."); break;
            case 3819: System.out.println("Error: score must be between 0 and 100.");                  break;
            case 1452: System.out.println("Error: student ID does not exist.");                        break;
            default:   System.out.println("Database error: " + e.getMessage());
        }
    }

    private static int readInt(Scanner in, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(in.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static double readScore(Scanner in, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double v = Double.parseDouble(in.nextLine().trim());
                if (v >= 0 && v <= 100) return v;
                System.out.println("Score must be between 0 and 100.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}