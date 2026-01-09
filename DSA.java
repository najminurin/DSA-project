import java.util.*;

public class DSA {

    /**
     * A Member node in the MLM tree.
     */
    public static class Member {
        private final String id;
        private final String name;
        private Member sponsor; // upline
        private Member previousSibling; // doubly-linked list among siblings
        private Member nextSibling;
        private final List<Member> downlines = new ArrayList<>();

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

        private void setSponsor(Member sponsor) {
            this.sponsor = sponsor;
        }

        private void addDownline(Member m) {
            // maintain doubly-linked sibling pointers
            if (!downlines.isEmpty()) {
                Member last = downlines.get(downlines.size() - 1);
                last.nextSibling = m;
                m.previousSibling = last;
            }
            downlines.add(m);
            m.setSponsor(this);
        }

        private void removeDownline(Member m) {
            int idx = downlines.indexOf(m);
            if (idx != -1) {
                Member prev = m.previousSibling;
                Member next = m.nextSibling;
                if (prev != null) prev.nextSibling = next;
                if (next != null) next.previousSibling = prev;
            }
            downlines.remove(m);
            m.previousSibling = null;
            m.nextSibling = null;
            m.setSponsor(null);
        }

        public List<Member> getDirectDownlines() {
            return Collections.unmodifiableList(downlines);
        }

        public List<Member> getAllDownlines() {
            List<Member> all = new ArrayList<>();
            for (Member d : downlines) {
                all.add(d);
                all.addAll(d.getAllDownlines());
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
            for (Member d : downlines) total += d.getSalesVolume();
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
            for (Member d : downlines) names.add(d.getName());
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
     * The MLM tree manager. Keeps an index of members by id and supports operations.
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
                    // root-level generated id
                    id = "M" + (members.size() + 1);
                }
            }

            if (members.containsKey(id)) {
                // ensure uniqueness
                int suffix = 1;
                String base = id;
                while (members.containsKey(id)) {
                    id = base + "-" + suffix++;
                }
            }

            Member m = new Member(id, name);
            if (sponsorId == null) {
                // treat as root / top-level
                if (root == null) {
                    root = m;
                } else {
                    // attach to root by default
                    root.addDownline(m);
                }
            } else {
                Member sponsor = members.get(sponsorId);
                if (sponsor == null) {
                    throw new IllegalArgumentException("Sponsor not found: " + sponsorId);
                }
                sponsor.addDownline(m);
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
         * percentages: level 1 (immediate sponsor) first, then level 2, etc.
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
         * add to ownSales and distribute uplines using provided percentages.
         */
        public void recordSale(String sellerId, double amount, List<Double> uplinePercentages) {
            Member seller = find(sellerId);
            if (seller == null) throw new IllegalArgumentException("Seller not found: " + sellerId);
            if (seller.getStatus() != Member.Status.ACTIVE) {
                System.out.println("Cannot record sale: seller is not active.");
                return;
            }

            // seller keeps own commission based on their rate
            double selfCommission = amount * seller.getCommissionRate();
            seller.addOwnSales(amount);
            seller.credit(selfCommission);
            System.out.printf("Seller %s receives own commission %.2f (%.2f%%)\n", seller.getName(), selfCommission, seller.getCommissionRate()*100);

            // distribute to uplines
            distributeCommission(sellerId, amount, uplinePercentages);
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
         * Print a textual (ASCII) visual representation of the tree, then a brief list of members.
         */
        public void printTreeVisual() {
            List<Member> roots = getTopLevelMembers();
            if (roots.isEmpty()) { System.out.println("<empty tree>"); return; }
            for (int i = 0; i < roots.size(); i++) {
                Member r = roots.get(i);
                // print root name
                System.out.println(r.getName());
                // print children recursively
                printVisual(r, "", true);
                if (i < roots.size() - 1) System.out.println();
            }
        }

        private void printVisual(Member node, String prefix, boolean isTail) {
            List<Member> children = node.getDirectDownlines();
            for (int i = 0; i < children.size(); i++) {
                Member child = children.get(i);
                boolean last = (i == children.size() - 1);
                System.out.printf("%s%s %s%n", prefix, (last ? "└──" : "├──"), child.getName());
                printVisual(child, prefix + (last ? "    " : "│   "), last);
            }
        }

        public List<Member> getTopLevelMembers() {
            List<Member> roots = new ArrayList<>();
            for (Member m : members.values()) {
                if (m.getSponsor() == null) roots.add(m);
            }
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
         * Reparent an existing member under a new parent.
         */
        public void reparentMember(String memberId, String newParentId) {
            Member child = members.get(memberId);
            Member newParent = members.get(newParentId);
            if (child == null) throw new IllegalArgumentException("Member not found: " + memberId);
            if (newParent == null) throw new IllegalArgumentException("New parent not found: " + newParentId);
            Member oldParent = child.getSponsor();
            if (oldParent != null) oldParent.removeDownline(child);
            newParent.addDownline(child);
        }

        /**
         * Save members to a TSV file. Simple, robust format with header.
         */
        public void saveToFile(String filename) throws java.io.IOException {
            try (java.io.BufferedWriter w = new java.io.BufferedWriter(new java.io.FileWriter(filename))) {
                w.write("id\tname\tsponsorId\townSales\tcommissionRate\tstatus\tphone\tbalance\n");
                for (Member m : members.values()) {
                    String sponsorId = m.getSponsor() == null ? "" : m.getSponsor().getId();
                    String nameEsc = m.getName().replace('\t', ' ').replace('\n', ' ');
                    String phoneEsc = m.getPhone().replace('\t', ' ').replace('\n', ' ');
                    w.write(String.join("\t",
                        m.getId(), nameEsc, sponsorId, String.valueOf(m.getOwnSales()), String.valueOf(m.getCommissionRate()), m.getStatus().name(), phoneEsc, String.valueOf(m.getBalance())));
                    w.newLine();
                }
            }
        }

        /**
         * Load members from TSV file. Returns true if file loaded.
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
                if (first && line.startsWith("id\t")) { first = false; continue; }
                String[] parts = line.split("\t", -1);
                if (parts.length < 8) continue;
                String id = parts[0];
                String name = parts[1];
                String sponsorId = parts[2].isEmpty() ? null : parts[2];
                double ownSales = parts[3].isEmpty() ? 0.0 : Double.parseDouble(parts[3]);
                double commissionRate = parts[4].isEmpty() ? 0.0 : Double.parseDouble(parts[4]);
                Member.Status status = Member.Status.valueOf(parts[5]);
                String phone = parts[6];
                double balance = parts[7].isEmpty() ? 0.0 : Double.parseDouble(parts[7]);

                Member m = new Member(id, name);
                m.addOwnSales(ownSales);
                m.setCommissionRate(commissionRate);
                m.setStatus(status);
                m.setPhone(phone);
                m.credit(balance);
                members.put(id, m);
                sponsorMap.put(id, sponsorId == null ? "" : sponsorId);
            }
            // attach children to sponsors
            for (Map.Entry<String, String> e : sponsorMap.entrySet()) {
                String id = e.getKey();
                String sponsorId = e.getValue();
                Member m = members.get(id);
                if (sponsorId == null || sponsorId.isEmpty()) {
                    if (root == null) root = m;
                } else {
                    Member sponsor = members.get(sponsorId);
                    if (sponsor != null) sponsor.addDownline(m);
                    else if (root == null) root = m;
                }
            }
            return true;
        }
    }

    public static void main(String[] args) {
        final String DATA_FILE = "mlm_data.tsv";
        MLMTree tree = new MLMTree();

        // Attempt to load existing data; if load fails or file missing, start with empty tree and go straight to interactive mode
        boolean loaded = false;
        try {
            if (new java.io.File(DATA_FILE).exists()) {
                loaded = tree.loadFromFile(DATA_FILE);
                if (loaded) System.out.println("Loaded data from " + DATA_FILE);
            }
        } catch (Exception ex) {
            System.out.println("Warning: Could not load data (" + ex.getMessage() + "). Starting with an empty tree.");
        }

        // Run tests only if explicitly requested via arg 'test'
        if (args.length > 0 && "test".equalsIgnoreCase(args[0])) {
            System.out.println("Running basic tests...");
            runTests();
        }

        Scanner sc = new Scanner(System.in);

        // If there's no data loaded and no members, go straight to interactive mode as requested
        if (!loaded || tree.getMembersSortedBySales().isEmpty()) {
            System.out.println("No data found — entering interactive mode so you can create your MLM structure.");
            runInteractive(tree, sc);
        } else {
            System.out.println("Data already present. Enter interactive mode? (y/n)");
            String ans = sc.nextLine().trim().toLowerCase();
            if (ans.equals("y") || ans.equals("yes")) runInteractive(tree, sc);
            else System.out.println("Exiting. You can run with no data next time to create a new structure.");
        }

        // Before exit, prompt to save
        while (true) {
            System.out.print("Save data to " + DATA_FILE + "? (y/n): ");
            String s = sc.nextLine().trim().toLowerCase();
            if (s.equals("y") || s.equals("yes")) {
                try { tree.saveToFile(DATA_FILE); System.out.println("Data saved to " + DATA_FILE); } catch (Exception ex) { System.out.println("Save failed: " + ex.getMessage()); }
                break;
            } else if (s.equals("n") || s.equals("no") || s.isEmpty()) { break; }
            else System.out.println("Please enter y or n.");
        }

        sc.close();
    }

    // Basic, self-contained tests (no framework required)
    private static void runTests() {
        boolean ok = true;
        MLMTree t = new MLMTree();
        t.addMember("C","Company",null);
        t.addMember("A","Alice","C");
        t.addMember("B","Bob","A");
        t.addMember("E","Eve","B");

        // Eve sells 200. Expect Bob 20 (10%), Alice 10 (5%)
        t.distributeCommission("E", 200.0, Arrays.asList(0.10, 0.05));
        double bob = t.find("B").getBalance();
        double alice = t.find("A").getBalance();
        if (Math.abs(bob - 20.0) > 1e-6) { System.out.println("Test failed: Bob expected 20, got "+bob); ok=false; }
        if (Math.abs(alice - 10.0) > 1e-6) { System.out.println("Test failed: Alice expected 10, got "+alice); ok=false; }

        if (ok) System.out.println("All tests passed.");
    }

    private static void runInteractive(MLMTree tree, Scanner sc) {
        System.out.println("--- Interactive MLM Editor ---");
        boolean running = true;
        while (running) {
            System.out.println("\nChoose an option:");
            System.out.println("1) Add Member");
            System.out.println("2) Make Sale");
            System.out.println("3) Show Member Details");
            System.out.println("4) Print Tree");
            System.out.println("5) List Members by Sales Volume");
            System.out.println("6) Add Parent to Member");
            System.out.println("7) Update Member (phone/commission/status)");
            System.out.println("8) Save to file");
            System.out.println("9) Load from file");
            System.out.println("10) Exit");
            System.out.print("> ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    addMemberInteractive(tree, sc);
                    break;
                case "2":
                    makeSaleInteractive(tree, sc);
                    break;
                case "3":
                    showMemberInteractive(tree, sc);
                    break;
                case "4":
                    // Print a visual ASCII tree and then a brief list of each member
                    tree.printTreeVisual();
                    System.out.println();
                    System.out.println("Brief details:");
                    for (Member m : tree.getAllMembersBFS()) {
                        System.out.printf("%s (%s) parent=%s children=%d level=%d sales=%.2f bal=%.2f\n",
                            m.getName(), m.getId(), m.parentName(), m.getDirectDownlines().size(), m.getLevel(), m.getSalesVolume(), m.getBalance());
                    }
                    break;
                case "5":
                    List<Member> sorted = tree.getMembersSortedBySales();
                    System.out.println("Members sorted by sales volume:");
                    for (Member m : sorted) System.out.printf("%s - Sales: %.2f\n", m.getName(), m.getSalesVolume());
                    break;
                case "6":
                    System.out.print("Member ID to add parent for: ");
                    String childId = sc.nextLine().trim();
                    if (tree.find(childId) == null) { System.out.println("Member not found."); break; }
                    addParentInteractive(tree, sc, childId);
                    break;
                case "7":
                    System.out.print("Member ID to update: ");
                    String updateId = sc.nextLine().trim();
                    if (tree.find(updateId) == null) { System.out.println("Member not found."); break; }
                    updateMemberInteractive(tree, sc, updateId);
                    break;
                case "8":
                    System.out.print("Filename to save to (leave empty for 'mlm_data.tsv'): ");
                    String saveFile = sc.nextLine().trim();
                    if (saveFile.isEmpty()) saveFile = "mlm_data.tsv";
                    try { tree.saveToFile(saveFile); System.out.println("Saved to " + saveFile); } catch (Exception ex) { System.out.println("Save failed: " + ex.getMessage()); }
                    break;
                case "9":
                    System.out.print("Filename to load (leave empty for 'mlm_data.tsv'): ");
                    String loadFile = sc.nextLine().trim();
                    if (loadFile.isEmpty()) loadFile = "mlm_data.tsv";
                    try { boolean ok = tree.loadFromFile(loadFile); if (ok) System.out.println("Loaded " + loadFile); else System.out.println("Load failed or file not found."); } catch (Exception ex) { System.out.println("Load failed: " + ex.getMessage()); }
                    break;
                case "10":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
        System.out.println("Leaving interactive mode.");
    }

    private static void addMemberInteractive(MLMTree tree, Scanner sc) {
        System.out.println("Follow the template. Leave ID empty to auto-generate.");
        String name;
        while (true) {
            System.out.print("Name: ");
            name = sc.nextLine().trim();
            if (!name.isEmpty()) break;
            System.out.println("Name cannot be empty.");
        }

        String id;
        while (true) {
            System.out.print("ID (optional): ");
            id = sc.nextLine().trim();
            if (id.isEmpty()) { id = null; break; }
            if (tree.find(id) != null) { System.out.println("ID already exists. Choose another or leave empty."); }
            else break;
        }

        String sponsorId;
        while (true) {
            System.out.print("Parent ID (leave empty for root): ");
            sponsorId = sc.nextLine().trim();
            if (sponsorId.isEmpty()) { sponsorId = null; break; }
            if (tree.find(sponsorId) == null) System.out.println("Parent not found. Try again or leave empty.");
            else break;
        }

        double commissionRate = 0.0;
        while (true) {
            System.out.print("Commission rate (percent, e.g. 10 for 10%): ");
            String s = sc.nextLine().trim();
            try {
                if (s.isEmpty()) { commissionRate = 0.0; break; }
                commissionRate = Double.parseDouble(s) / 100.0;
                if (commissionRate < 0 || commissionRate > 1) { System.out.println("Enter a percent between 0 and 100."); continue; }
                break;
            } catch (NumberFormatException ex) { System.out.println("Invalid number."); }
        }

        String phone = "";
        while (true) {
            System.out.print("Phone (digits, +, -, spaces allowed) (optional): ");
            phone = sc.nextLine().trim();
            if (phone.isEmpty()) break;
            if (!phone.matches("[0-9()+\\- ]{6,20}")) { System.out.println("Invalid phone. Try again."); }
            else break;
        }

        Member m = tree.addMember(id, name, sponsorId);
        m.setCommissionRate(commissionRate);
        m.setPhone(phone);

        System.out.println("Member added:\n" + m.detailsString());

        // Ask whether to add a child now
        while (true) {
            System.out.print("Add a child to this member? (y/n): ");
            String a = sc.nextLine().trim().toLowerCase();
            if (a.equals("y") || a.equals("yes")) { addMemberInteractiveChild(tree, sc, m.getId()); break; }
            else if (a.equals("n") || a.equals("no")) break;
            else System.out.println("Please enter 'y' or 'n'.");
        }

        // Ask whether to add a parent now (create new parent and attach this member under it)
        while (true) {
            System.out.print("Add a parent to this member? (y/n): ");
            String a = sc.nextLine().trim().toLowerCase();
            if (a.equals("y") || a.equals("yes")) { addParentInteractive(tree, sc, m.getId()); break; }
            else if (a.equals("n") || a.equals("no") || a.isEmpty()) break;
            else System.out.println("Please enter 'y' or 'n'.");
        }
    }

    private static void addMemberInteractiveChild(MLMTree tree, Scanner sc, String parentId) {
        System.out.println("Adding a child for parent ID: " + parentId);
        System.out.print("Child Name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) { System.out.println("Name cannot be empty. Aborting child add."); return; }
        // auto-generate child ID based on parent
        Member parent = tree.find(parentId);
        String childId = null; // let addMember auto-generate
        Member child = tree.addMember(childId, name, parentId);
        System.out.println("Child added: " + child.detailsString());

        // Ask whether to continue adding children to this child
        while (true) {
            System.out.print("Add a child to the newly created child? (y/n): ");
            String a = sc.nextLine().trim().toLowerCase();
            if (a.equals("y") || a.equals("yes")) { addMemberInteractiveChild(tree, sc, child.getId()); break; }
            else if (a.equals("n") || a.equals("no") || a.isEmpty()) break;
            else System.out.println("Please enter 'y' or 'n'.");
        }
    }

    private static void makeSaleInteractive(MLMTree tree, Scanner sc) {
        System.out.print("Seller ID: ");
        String sellerId = sc.nextLine().trim();
        Member seller = tree.find(sellerId);
        if (seller == null) { System.out.println("Seller not found."); return; }
        System.out.print("Sale amount: ");
        String amtS = sc.nextLine().trim();
        double amt;
        try { amt = Double.parseDouble(amtS); if (amt <= 0) { System.out.println("Amount must be positive."); return; } }
        catch (NumberFormatException ex) { System.out.println("Invalid number."); return; }

        System.out.println("Enter upline percentages per level as comma-separated percents (e.g. 10,5,2) or leave empty for default 10,5,2:");
        String perS = sc.nextLine().trim();
        List<Double> percs = new ArrayList<>();
        if (perS.isEmpty()) percs = Arrays.asList(0.10, 0.05, 0.02);
        else {
            String[] parts = perS.split(",");
            for (String p : parts) {
                try { double v = Double.parseDouble(p.trim())/100.0; percs.add(v); } catch (NumberFormatException ex) { System.out.println("Invalid percent: " + p); return; }
            }
        }
        tree.recordSale(sellerId, amt, percs);

        // Ask whether user wants to continue making children/parent creations for seller
        while (true) {
            System.out.print("Create a child or parent for this seller? (child/parent/none): ");
            String ans = sc.nextLine().trim().toLowerCase();
            if (ans.equals("child")) { addMemberInteractiveChild(tree, sc, sellerId); break; }
            else if (ans.equals("parent")) { addParentInteractive(tree, sc, sellerId); break; }
            else if (ans.equals("none") || ans.isEmpty()) break;
            else System.out.println("Enter 'child', 'parent' or 'none'.");
        }
    }

    private static void showMemberInteractive(MLMTree tree, Scanner sc) {
        System.out.print("Member ID: ");
        String id = sc.nextLine().trim();
        Member m = tree.find(id);
        if (m == null) { System.out.println("Member not found."); return; }
        System.out.println(m.detailsString());

        // Ask to perform quick actions
        while (true) {
            System.out.print("Perform action on this member (add-child/add-parent/update/none): ");
            String a = sc.nextLine().trim().toLowerCase();
            if (a.equals("add-child")) { addMemberInteractiveChild(tree, sc, id); break; }
            else if (a.equals("add-parent")) { addParentInteractive(tree, sc, id); break; }
            else if (a.equals("update")) { updateMemberInteractive(tree, sc, id); break; }
            else if (a.equals("none") || a.isEmpty()) break;
            else System.out.println("Enter one of: add-child, add-parent, update, none.");
        }
    }

    private static void addParentInteractive(MLMTree tree, Scanner sc, String childId) {
        System.out.println("Creating a new parent for " + childId);
        System.out.print("Parent Name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) { System.out.println("Name cannot be empty."); return; }
        String id = null; // let addMember generate
        Member parent = tree.addMember(id, name, null);
        tree.reparentMember(childId, parent.getId());
        System.out.println("Parent created and child reparented:\n" + parent.detailsString());
    }

    private static void addParentInteractive(MLMTree tree, Scanner sc) {
        System.out.print("Child ID to attach new parent to: ");
        String childId = sc.nextLine().trim();
        if (tree.find(childId) == null) { System.out.println("Child not found."); return; }
        addParentInteractive(tree, sc, childId);
    }

    private static void updateMemberInteractive(MLMTree tree, Scanner sc, String memberId) {
        Member m = tree.find(memberId);
        if (m == null) { System.out.println("Member not found."); return; }
        System.out.println("Updating member: " + m.getName());
        while (true) {
            System.out.println("Choose field to update: phone / commission / status / done");
            System.out.print("> ");
            String f = sc.nextLine().trim().toLowerCase();
            if (f.equals("phone")) {
                System.out.print("New phone: ");
                String phone = sc.nextLine().trim();
                if (!phone.matches("[0-9()+\\- ]{6,20}")) { System.out.println("Invalid phone format."); }
                else { m.setPhone(phone); System.out.println("Phone updated."); }
            } else if (f.equals("commission")) {
                System.out.print("Commission percent (e.g. 10): ");
                String s = sc.nextLine().trim();
                try { double pct = Double.parseDouble(s); if (pct < 0 || pct > 100) { System.out.println("Enter 0-100."); } else { m.setCommissionRate(pct/100.0); System.out.println("Commission updated."); } }
                catch (NumberFormatException ex) { System.out.println("Invalid number."); }
            } else if (f.equals("status")) {
                System.out.print("Status (ACTIVE/INACTIVE/TERMINATED): ");
                String st = sc.nextLine().trim().toUpperCase();
                try { m.setStatus(Member.Status.valueOf(st)); System.out.println("Status updated."); } catch (IllegalArgumentException ex) { System.out.println("Invalid status."); }
            } else if (f.equals("done")) { break; }
            else System.out.println("Unknown field.");
        }
        System.out.println("Updated member:\n" + m.detailsString());
    }
}
