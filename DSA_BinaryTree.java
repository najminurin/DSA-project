import java.util.*;

public class DSA_BinaryTree {

    /**
     * A Member node in the MLM Binary Tree.
     * Each member can have at most 2 child nodes (left and right).
     */
    public static class Member {
        private final String id;
        private final String name;
        private Member sponsor; // upline/parent
        private Member leftChild;
        private Member rightChild;

        // Business fields
        private double balance = 0.0; // earnings / commissions
        private double ownSales = 0.0; // sales made by this member
        private double commissionRate = 0.0; // fraction of own sale credited to self
        private Status status = Status.ACTIVE;
        private String phone = "";

        public enum Status { ACTIVE, INACTIVE, TERMINATED }

        public Member(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public Member getSponsor() { return sponsor; }
        public double getBalance() { return balance; }
        public double getOwnSales() { return ownSales; }
        public double getCommissionRate() { return commissionRate; }
        public Status getStatus() { return status; }
        public String getPhone() { return phone; }
        public Member getLeftChild() { return leftChild; }
        public Member getRightChild() { return rightChild; }

        private void setSponsor(Member sponsor) {
            this.sponsor = sponsor;
        }

        /**
         * Add a downline child. Can only have max 2 children (left and right).
         * Returns true if added successfully, false if already has 2 children.
         */
        private boolean addDownline(Member m) {
            if (leftChild == null) {
                leftChild = m;
                m.setSponsor(this);
                return true;
            } else if (rightChild == null) {
                rightChild = m;
                m.setSponsor(this);
                return true;
            }
            // Cannot add more than 2 children
            return false;
        }

        private void removeDownline(Member m) {
            if (m == leftChild) {
                leftChild = null;
                m.setSponsor(null);
            } else if (m == rightChild) {
                rightChild = null;
                m.setSponsor(null);
            }
        }

        public List<Member> getDirectDownlines() {
            List<Member> children = new ArrayList<>();
            if (leftChild != null) children.add(leftChild);
            if (rightChild != null) children.add(rightChild);
            return Collections.unmodifiableList(children);
        }

        public List<Member> getAllDownlines() {
            List<Member> all = new ArrayList<>();
            if (leftChild != null) {
                all.add(leftChild);
                all.addAll(leftChild.getAllDownlines());
            }
            if (rightChild != null) {
                all.add(rightChild);
                all.addAll(rightChild.getAllDownlines());
            }
            return all;
        }

        public List<Member> getUplines() {
            List<Member> ups = new ArrayList<>();
            Member cur = this.sponsor;
            while (cur != null) {
                ups.add(cur);
                cur = cur.sponsor;
            }
            return ups;
        }

        public int getLevel() {
            int level = 0;
            Member cur = this.sponsor;
            while (cur != null) { level++; cur = cur.sponsor; }
            return level;
        }

        public double getSalesVolume() {
            double total = this.ownSales;
            if (leftChild != null) total += leftChild.getSalesVolume();
            if (rightChild != null) total += rightChild.getSalesVolume();
            return total;
        }

        public void addOwnSales(double amount) {
            this.ownSales += amount;
        }

        public void setCommissionRate(double rate) {
            this.commissionRate = rate;
        }

        public void setStatus(Status s) {
            this.status = s;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public void credit(double amount) {
            this.balance += amount;
        }

        public String parentName() {
            return sponsor == null ? "<none>" : sponsor.getName();
        }

        public List<String> childrenNames() {
            List<String> names = new ArrayList<>();
            if (leftChild != null) names.add("L:" + leftChild.getName());
            if (rightChild != null) names.add("R:" + rightChild.getName());
            return names;
        }

        public String detailsString() {
            return String.format(
                "Name: %s\nID: %s\nParent Name: %s\nChildren: %s\nLevel/Depth: %d\nSales Volume: %.2f\nCommission Rate: %.2f%%\nStatus: %s\nPhone: %s\nBalance: %.2f",
                getName(), getId(), parentName(), childrenNames(), getLevel(), getSalesVolume(), getCommissionRate()*100, getStatus(), getPhone(), getBalance()
            );
        }

        @Override
        public String toString() {
            return String.format("%s (%s) - balance: %.2f", name, id, balance);
        }
    }

    /**
     * The MLM Binary Tree manager. Keeps an index of members by id and supports operations.
     * Each member can have at most 2 children (left and right).
     */
    public static class MLMTree {
        private final Map<String, Member> members = new HashMap<>();
        private Member root; // optional top-level node (company or admin)

        public Member addMember(String id, String name, String sponsorId) {
            // Auto-generate id if not provided
            if (id == null || id.trim().isEmpty()) {
                if (sponsorId != null) {
                    Member sponsor = members.get(sponsorId);
                    if (sponsor == null) throw new IllegalArgumentException("Sponsor not found: " + sponsorId);
                    id = sponsorId + "-" + (sponsor.getDirectDownlines().size() + 1);
                } else {
                    id = "M" + (members.size() + 1);
                }
            }

            if (members.containsKey(id)) {
                int suffix = 1;
                String base = id;
                while (members.containsKey(id)) id = base + "-" + (suffix++);
            }

            Member m = new Member(id, name);
            if (sponsorId == null) {
                if (root == null) {
                    root = m;
                } else {
                    throw new IllegalArgumentException("Tree already has a root. Cannot add another root-level member.");
                }
            } else {
                Member sponsor = members.get(sponsorId);
                if (sponsor == null) throw new IllegalArgumentException("Sponsor not found: " + sponsorId);
                if (!sponsor.addDownline(m)) {
                    throw new IllegalArgumentException("Sponsor " + sponsorId + " already has 2 children (binary tree limit reached).");
                }
            }
            members.put(id, m);
            return m;
        }

        public Member find(String id) {
            return members.get(id);
        }

        public List<Member> getUplines(String id) {
            Member m = find(id);
            if (m == null) return Collections.emptyList();
            return m.getUplines();
        }

        public List<Member> getDownlines(String id) {
            Member m = find(id);
            if (m == null) return Collections.emptyList();
            return m.getAllDownlines();
        }

        /**
         * Distribute a sale's commission up the upline.
         */
        public void distributeCommission(String sellerId, double saleAmount, List<Double> percentages) {
            Member seller = find(sellerId);
            if (seller == null) throw new IllegalArgumentException("Seller not found: " + sellerId);
            List<Member> ups = seller.getUplines();
            for (int i = 0; i < percentages.size() && i < ups.size(); i++) {
                double pct = percentages.get(i);
                double commission = saleAmount * pct;
                ups.get(i).credit(commission);
                System.out.printf("Level %d upline %s receives %.2f (%.2f%%)\n", i+1, ups.get(i).getName(), commission, pct*100);
            }
        }

        /**
         * Record a sale made by a seller: credit seller's own commission based on their commissionRate,
         * add to ownSales and distribute uplines automatically.
         */
        public void recordSale(String sellerId, double amount) {
            Member seller = find(sellerId);
            if (seller == null) throw new IllegalArgumentException("Seller not found: " + sellerId);
            if (seller.getStatus() != Member.Status.ACTIVE) {
                System.out.println("Cannot record sale: seller is not active.");
                return;
            }

            double selfCommission = amount * seller.getCommissionRate();
            seller.addOwnSales(amount);
            seller.credit(selfCommission);
            System.out.printf("Seller %s receives own commission %.2f (%.2f%%)\n", seller.getName(), selfCommission, seller.getCommissionRate()*100);

            List<Member> ups = seller.getUplines();
            if (ups.isEmpty()) return;
            double distributionSum = amount * (1.0 - seller.getCommissionRate());
            List<Double> percs = generateUplinePercentages(ups.size());
            List<Member> upsReversed = new ArrayList<>(ups);
            Collections.reverse(upsReversed);
            for (int i = 0; i < percs.size() && i < upsReversed.size(); i++) {
                double pct = percs.get(i);
                double commission = distributionSum * pct;
                upsReversed.get(i).credit(commission);
                System.out.printf("Upline level %d (%s) receives %.2f (%.2f%%)\n", i+1, upsReversed.get(i).getName(), commission, pct*100);
            }
        }

        /**
         * Generate upline distribution percentages based on number of uplines.
         * Uses square-root weighting so root receives largest share.
         */
        private List<Double> generateUplinePercentages(int k) {
            List<Double> out = new ArrayList<>();
            if (k <= 0) return out;
            double p = 0.5;
            double sum = 0.0;
            for (int i = 0; i < k; i++) {
                double weight = Math.pow((double)(k - i), p);
                out.add(weight);
                sum += weight;
            }
            for (int i = 0; i < k; i++) out.set(i, out.get(i) / sum);
            return out;
        }

        /**
         * Get all members sorted by descending sales volume using merge sort
         */
        public List<Member> getMembersSortedBySales() {
            List<Member> list = new ArrayList<>(members.values());
            return mergeSortBySales(list);
        }

        private List<Member> mergeSortBySales(List<Member> list) {
            if (list.size() <= 1) return list;
            int mid = list.size()/2;
            List<Member> left = mergeSortBySales(new ArrayList<>(list.subList(0, mid)));
            List<Member> right = mergeSortBySales(new ArrayList<>(list.subList(mid, list.size())));
            return mergeBySales(left, right);
        }

        private List<Member> mergeBySales(List<Member> a, List<Member> b) {
            List<Member> out = new ArrayList<>();
            int i = 0, j = 0;
            while (i < a.size() && j < b.size()) {
                if (a.get(i).getSalesVolume() >= b.get(j).getSalesVolume()) out.add(a.get(i++));
                else out.add(b.get(j++));
            }
            while (i < a.size()) out.add(a.get(i++));
            while (j < b.size()) out.add(b.get(j++));
            return out;
        }

        /**
         * Print tree breadth-first.
         */
        public void printTree() {
            if (root == null) {
                System.out.println("<empty tree>");
                return;
            }
            Queue<Member> q = new LinkedList<>();
            q.add(root);
            while (!q.isEmpty()) {
                Member m = q.poll();
                System.out.printf("%s (%s) -> downlines: %d | balance: %.2f\n", m.getName(), m.getId(), m.getDirectDownlines().size(), m.getBalance());
                for (Member d : m.getDirectDownlines()) q.add(d);
            }
        }

        /**
         * Print a textual (ASCII) visual representation of the binary tree.
         */
        public void printTreeVisual() {
            if (root == null) { System.out.println("<empty tree>"); return; }
            System.out.println(root.getName());
            printVisual(root, "", true);
        }

        private void printVisual(Member node, String prefix, boolean isTail) {
            List<Member> children = node.getDirectDownlines();
            for (int i = 0; i < children.size(); i++) {
                Member child = children.get(i);
                boolean last = (i == children.size() - 1);
                String childLabel = (node.getLeftChild() == child) ? "[L] " : "[R] ";
                System.out.printf("%s%s %s%s%n", prefix, (last ? "└──" : "├──"), childLabel, child.getName());
                printVisual(child, prefix + (last ? "    " : "│   "), last);
            }
        }

        public List<Member> getTopLevelMembers() {
            List<Member> roots = new ArrayList<>();
            if (root != null) roots.add(root);
            return roots;
        }

        public List<Member> getAllMembersBFS() {
            List<Member> out = new ArrayList<>();
            Queue<Member> q = new LinkedList<>();
            for (Member r : getTopLevelMembers()) q.add(r);
            while (!q.isEmpty()) {
                Member m = q.poll();
                out.add(m);
                for (Member d : m.getDirectDownlines()) q.add(d);
            }
            return out;
        }

        public void printBalances() {
            System.out.println("--- Balances ---");
            for (Member m : members.values()) {
                System.out.println(m);
            }
        }

        /**
         * Reparent an existing member under a new parent (binary tree compatible).
         */
        public void reparentMember(String memberId, String newParentId) {
            Member child = members.get(memberId);
            Member newParent = members.get(newParentId);
            if (child == null) throw new IllegalArgumentException("Member not found: " + memberId);
            if (newParent == null) throw new IllegalArgumentException("New parent not found: " + newParentId);
            Member oldParent = child.getSponsor();
            if (oldParent != null) oldParent.removeDownline(child);
            if (!newParent.addDownline(child)) {
                throw new IllegalArgumentException("New parent already has 2 children (binary tree limit).");
            }
        }

        /**
         * Insert a new parent above an existing member.
         */
        public Member addParentAbove(String childId, String parentName) {
            Member child = members.get(childId);
            if (child == null) throw new IllegalArgumentException("Child not found: " + childId);
            Member oldSponsor = child.getSponsor();

            String id;
            if (oldSponsor != null) {
                id = oldSponsor.getId() + "-" + (oldSponsor.getDirectDownlines().size() + 1);
            } else {
                id = "M" + (members.size() + 1);
            }
            if (members.containsKey(id)) {
                int suffix = 1;
                String base = id;
                while (members.containsKey(id)) id = base + "-" + (suffix++);
            }

            Member parent = new Member(id, parentName);
            members.put(id, parent);

            if (oldSponsor == null) {
                parent.addDownline(child);
                if (root == child) root = parent;
            } else {
                oldSponsor.removeDownline(child);
                oldSponsor.addDownline(parent);
                parent.addDownline(child);
            }
            return parent;
        }

        /**
         * Save members to a TSV file.
         */
        public void saveToFile(String filename) throws java.io.IOException {
            try (java.io.BufferedWriter w = new java.io.BufferedWriter(new java.io.FileWriter(filename))) {
                w.write("id\tname\tsponsorId\townSales\tcommissionRate\tstatus\tphone\tbalance\n");
                for (Member m : members.values()) {
                    String sponsorId = (m.getSponsor() == null) ? "" : m.getSponsor().getId();
                    w.write(String.format("%s\t%s\t%s\t%.2f\t%.2f\t%s\t%s\t%.2f\n",
                        m.getId(), m.getName(), sponsorId, m.getOwnSales(), m.getCommissionRate(), m.getStatus(), m.getPhone(), m.getBalance()));
                }
            }
        }

        /**
         * Load members from TSV file.
         */
        public boolean loadFromFile(String filename) throws java.io.IOException {
            java.io.File f = new java.io.File(filename);
            if (!f.exists()) return false;
            java.util.List<String> lines = java.nio.file.Files.readAllLines(f.toPath());
            members.clear();
            root = null;
            Map<String, String> sponsorMap = new HashMap<>();
            boolean first = true;
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                if (first) { first = false; continue; }
                String[] parts = line.split("\t");
                if (parts.length < 8) continue;
                String id = parts[0];
                String name = parts[1];
                String sponsorId = parts[2].isEmpty() ? null : parts[2];
                double ownSales = Double.parseDouble(parts[3]);
                double rate = Double.parseDouble(parts[4]);
                Member.Status status = Member.Status.valueOf(parts[5]);
                String phone = parts[6];
                double balance = Double.parseDouble(parts[7]);
                
                Member m = new Member(id, name);
                m.addOwnSales(ownSales);
                m.setCommissionRate(rate);
                m.setStatus(status);
                m.setPhone(phone);
                m.credit(balance);
                members.put(id, m);
                sponsorMap.put(id, sponsorId);
            }
            // attach children to sponsors
            for (Map.Entry<String, String> e : sponsorMap.entrySet()) {
                Member child = members.get(e.getKey());
                String sponsorId = e.getValue();
                if (sponsorId != null) {
                    Member sponsor = members.get(sponsorId);
                    if (sponsor != null) {
                        if (!sponsor.addDownline(child)) {
                            System.err.println("Warning: Could not add " + child.getName() + " to " + sponsor.getName() + " (binary tree limit reached).");
                        }
                    }
                } else {
                    root = child;
                }
            }
            return true;
        }
    }

    private static boolean isSaved = true;

    public static void main(String[] args) {
        final String DATA_FILE = "mlm_data.tsv";
        MLMTree tree = new MLMTree();

        boolean loaded = false;
        try {
            if (new java.io.File(DATA_FILE).exists()) {
                loaded = tree.loadFromFile(DATA_FILE);
                if (loaded) System.out.println("Data loaded successfully.");
            }
        } catch (Exception ex) {
            System.out.println("Warning: Could not load data (" + ex.getMessage() + "). Starting with an empty tree.");
        }

        if (args.length > 0 && "test".equalsIgnoreCase(args[0])) {
            System.out.println("Running basic tests...");
            runTests();
        }

        Scanner sc = new Scanner(System.in);

        if (!loaded || tree.getMembersSortedBySales().isEmpty()) {
            System.out.println("No data found — entering interactive mode.");
            runInteractive(tree, sc);
        } else {
            System.out.println("Data already present. Enter interactive mode? (y/n)");
            String ans = sc.nextLine().trim().toLowerCase();
            if (ans.equals("y") || ans.equals("yes")) {
                runInteractive(tree, sc);
            } else {
                System.out.println("Staying in view mode.");
            }
        }

        while (true) {
            System.out.print("Save data to " + DATA_FILE + "? (y/n): ");
            String s = sc.nextLine().trim().toLowerCase();
            if (s.equals("y") || s.equals("yes")) {
                try { tree.saveToFile(DATA_FILE); System.out.println("Saved."); break; }
                catch (Exception ex) { System.out.println("Error saving: " + ex.getMessage()); }
            } else if (s.equals("n") || s.equals("no")) {
                break;
            }
        }

        sc.close();
    }

    private static void runTests() {
        System.out.println("Test 1: Basic binary tree structure");
        MLMTree t = new MLMTree();
        t.addMember("C", "Company", null);
        t.addMember("A", "Alice", "C");
        t.addMember("B", "Bob", "C");
        System.out.println("Added Company with 2 children (Alice, Bob)");
        
        try {
            t.addMember("D", "David", "C");
            System.out.println("ERROR: Should not allow 3rd child!");
        } catch (IllegalArgumentException e) {
            System.out.println("Correctly rejected 3rd child: " + e.getMessage());
        }

        System.out.println("\nTest 2: Tree structure");
        t.printTreeVisual();
        System.out.println("\nAll tests completed.");
    }

    private static void runInteractive(MLMTree tree, Scanner sc) {
        System.out.println("--- Interactive MLM Binary Tree Editor (Max 2 children per node) ---");
        boolean running = true;
        while (running) {
            System.out.println("\nChoose an option:");
            System.out.println("1) Add Member (up to 2 per parent)");
            System.out.println("2) Show Member Details");
            System.out.println("3) Print Tree (Visual)");
            System.out.println("4) List Members by Sales Volume (Merge Sort)");
            System.out.println("5) Update Member");
            System.out.println("6) Save to file");
            System.out.println("7) Load from file");
            System.out.println("8) Exit");
            System.out.println("9) List All Members (full details)");
            System.out.print("> ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    addMemberInteractive(tree, sc);
                    break;
                case "2":
                    showMemberInteractive(tree, sc);
                    break;
                case "3":
                    tree.printTreeVisual();
                    break;
                case "4":
                    List<Member> sorted = tree.getMembersSortedBySales();
                    System.out.println("\n=== Members Sorted by Sales Volume (Merge Sort) ===");
                    if (sorted.isEmpty()) {
                        System.out.println("No members to display.");
                    } else {
                        System.out.printf("%-5s %-20s %-15s %-15s%n", "Rank", "Name", "Sales Volume", "Balance");
                        System.out.println("---------------------------------------------------------------");
                        int rank = 1;
                        for (Member m : sorted) {
                            System.out.printf("%-5d %-20s $%-14.2f $%-14.2f%n", rank++, m.getName(), m.getSalesVolume(), m.getBalance());
                        }
                    }
                    break;
                case "5":
                    System.out.print("Member ID to update: ");
                    String updateId = sc.nextLine().trim();
                    if (tree.find(updateId) == null) { System.out.println("Member not found."); break; }
                    updateMemberInteractive(tree, sc, updateId);
                    break;
                case "6":
                    try { tree.saveToFile("mlm_data.tsv"); System.out.println("Saved."); isSaved = true; }
                    catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
                    break;
                case "7":
                    try { tree.loadFromFile("mlm_data.tsv"); System.out.println("Loaded."); isSaved = true; }
                    catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
                    break;
                case "9":
                    List<Member> all = tree.getAllMembersBFS();
                    if (all.isEmpty()) { System.out.println("No members available."); break; }
                    System.out.println("\n--- All Members (full details) ---");
                    for (Member m : all) {
                        System.out.println(m.detailsString());
                        System.out.println("----------------------------");
                    }
                    break;
                case "8":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
        System.out.println("Leaving interactive mode.");
    }

    private static void addMemberInteractive(MLMTree tree, Scanner sc) {
        System.out.println("Adding a new member...");
        System.out.print("Name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) { System.out.println("Name cannot be empty."); return; }

        String sponsorId;
        boolean rootExists = !tree.getTopLevelMembers().isEmpty();
        if (rootExists) {
            System.out.print("Sponsor ID (or blank for root): ");
            sponsorId = sc.nextLine().trim();
            if (sponsorId.isEmpty()) sponsorId = null;
        } else {
            System.out.println("No root exists. Creating as root.");
            sponsorId = null;
        }

        if (sponsorId != null && tree.find(sponsorId) == null) {
            System.out.println("Sponsor not found.");
            return;
        }

        try {
            Member m = tree.addMember(null, name, sponsorId);
            isSaved = false;
            System.out.println("Member added:\n" + m.detailsString());
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void showMemberInteractive(MLMTree tree, Scanner sc) {
        System.out.print("Member ID: ");
        String id = sc.nextLine().trim();
        Member m = tree.find(id);
        if (m == null) { System.out.println("Member not found."); return; }
        System.out.println(m.detailsString());
    }

    private static void updateMemberInteractive(MLMTree tree, Scanner sc, String memberId) {
        Member m = tree.find(memberId);
        if (m == null) { System.out.println("Member not found."); return; }
        System.out.println("Updating member: " + m.getName());
        boolean changed = false;
        while (true) {
            System.out.println("Choose field: phone / commission / status / done");
            System.out.print("> ");
            String f = sc.nextLine().trim().toLowerCase();
            if (f.equals("phone")) {
                System.out.print("New phone: ");
                String p = sc.nextLine().trim();
                m.setPhone(p);
                changed = true;
            } else if (f.equals("commission")) {
                System.out.print("New commission rate (0-100): ");
                try { double r = Double.parseDouble(sc.nextLine().trim()) / 100.0; m.setCommissionRate(r); changed = true; }
                catch (NumberFormatException ex) { System.out.println("Invalid."); }
            } else if (f.equals("status")) {
                System.out.print("New status (ACTIVE/INACTIVE/TERMINATED): ");
                try { m.setStatus(Member.Status.valueOf(sc.nextLine().trim())); changed = true; }
                catch (Exception ex) { System.out.println("Invalid."); }
            } else if (f.equals("done")) {
                break;
            }
        }
        if (changed) isSaved = false;
        System.out.println("Updated member:\n" + m.detailsString());
    }
}
