import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentManager {
    // HashMap allows O(1) access time complexity for search operations
    private Map<String, Student> studentMap;
    
    public StudentManager() {
        this.studentMap = new HashMap<>();
    }

    // 1. Insertion Algorithm
    public void addStudent(String id, String name, double gpa) {
        if (studentMap.containsKey(id)) {
            System.out.println("Error: Student ID already exists.");
            return;
        }
        Student newStudent = new Student(id, name, gpa);
        studentMap.put(id, newStudent);
        System.out.println("Student added successfully.");
    }

    // 2. Search Algorithm
    public void findStudent(String id) {
        if (studentMap.containsKey(id)) {
            System.out.println("Found: " + studentMap.get(id));
        } else {
            System.out.println("Student not found.");
        }
    }

    // 3. Display Algorithm (Iterating)
    public void displayAll() {
        if (studentMap.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        System.out.println("\n--- Student Records ---");
        for (Student s : studentMap.values()) {
            System.out.println(s);
        }
        System.out.println("-----------------------\n");
    }
}