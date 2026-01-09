
    /**
 * Represents a single student entity.
 * Demonstrates Encapsulation.
 */
public class Student {
    private String studentId;
    private String name;
    private double gpa;

    public Student(String studentId, String name, double gpa) {
        this.studentId = studentId;
        this.name = name;
        this.gpa = gpa;
    }

    // Getters and Setters
    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public double getGpa() { return gpa; }

    @Override
    public String toString() {
        return String.format("ID: %s | Name: %-15s | GPA: %.2f", studentId, name, gpa);
    }
}

