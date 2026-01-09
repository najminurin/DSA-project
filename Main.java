import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        StudentManager manager = new StudentManager();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n1. Add Student");
            System.out.println("2. Find Student");
            System.out.println("3. View All");
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    System.out.print("Enter ID: ");
                    String id = scanner.nextLine();
                    System.out.print("Enter Name: ");
                    String name = scanner.nextLine();
                    System.out.print("Enter GPA: ");
                    double gpa = Double.parseDouble(scanner.nextLine());
                    manager.addStudent(id, name, gpa);
                    break;
                case "2":
                    System.out.print("Enter ID to search: ");
                    String searchId = scanner.nextLine();
                    manager.findStudent(searchId);
                    break;
                case "3":
                    manager.displayAll();
                    break;
                case "4":
                    running = false;
                    System.out.println("System shutting down...");
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
        scanner.close();
    }
}