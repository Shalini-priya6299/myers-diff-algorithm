import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Main {

    // -------------------- Operation --------------------

    static class Op {
        char type;       // K = keep, D = delete, I = insert
        int aIndex;
        int bIndex;

        Op(char type, int aIndex, int bIndex) {
            this.type = type;
            this.aIndex = aIndex;
            this.bIndex = bIndex;
        }
    }

    // -------------------- Reading files --------------------

    static byte[] readFile(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    static List<byte[]> getLines(byte[] data) {
        List<byte[]> lines = new ArrayList<>();

        int start = 0;

        for (int i = 0; i < data.length; i++) {
            if (data[i] == '\n') {
                lines.add(Arrays.copyOfRange(data, start, i));
                start = i + 1;
            }
        }

        // Do not add an extra empty line when the file ends with \n
        if (start < data.length) {
            lines.add(Arrays.copyOfRange(data, start, data.length));
        }

        return lines;
    }

    // -------------------- Line comparison --------------------

    static boolean sameLine(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }

    // -------------------- Myers Diff --------------------

    static List<Op> myers(List<byte[]> A, List<byte[]> B) {

        int N = A.size();
        int M = B.size();

        int maxD = N + M;
        int offset = maxD + 1;

        int[] V = new int[2 * maxD + 3];

        List<int[]> trace = new ArrayList<>();

        int finalD = 0;

        boolean finished = false;

        for (int d = 0; d <= maxD; d++) {

            trace.add(V.clone());

            for (int k = -d; k <= d; k += 2) {

                int index = offset + k;

                int x;

                // Choose whether to move down (insert)
                // or right (delete)
                if (k == -d ||
                    (k != d && V[index - 1] < V[index + 1])) {

                    x = V[index + 1];

                } else {

                    x = V[index - 1] + 1;
                }

                int y = x - k;

                // Snake: keep matching lines
                while (x < N &&
                       y < M &&
                       sameLine(A.get(x), B.get(y))) {

                    x++;
                    y++;
                }

                V[index] = x;

                if (x >= N && y >= M) {
                    finalD = d;
                    finished = true;
                    break;
                }
            }

            if (finished) {
                break;
            }
        }

        return backtrack(A, B, trace, finalD, offset);
    }

    // -------------------- Backtracking --------------------

    static List<Op> backtrack(
            List<byte[]> A,
            List<byte[]> B,
            List<int[]> trace,
            int finalD,
            int offset) {

        List<Op> result = new ArrayList<>();

        int x = A.size();
        int y = B.size();

        for (int d = finalD; d > 0; d--) {

            int[] V = trace.get(d);

            int k = x - y;

            int previousK;

            if (k == -d ||
                (k != d &&
                 V[offset + k - 1] < V[offset + k + 1])) {

                previousK = k + 1;

            } else {

                previousK = k - 1;
            }

            int previousX = V[offset + previousK];
            int previousY = previousX - previousK;

            // Move backwards through the snake
            while (x > previousX && y > previousY) {

                result.add(
                    new Op('K', x - 1, y - 1)
                );

                x--;
                y--;
            }

            // Find the edit
            if (x == previousX + 1) {

                // Delete from A
                result.add(
                    new Op('D', x - 1, -1)
                );

                x--;

            } else {

                // Insert from B
                result.add(
                    new Op('I', -1, y - 1)
                );

                y--;
            }
        }

        // Remaining matching lines
        while (x > 0 && y > 0) {

            result.add(
                new Op('K', x - 1, y - 1)
            );

            x--;
            y--;
        }

        while (x > 0) {

            result.add(
                new Op('D', x - 1, -1)
            );

            x--;
        }

        while (y > 0) {

            result.add(
                new Op('I', -1, y - 1)
            );

            y--;
        }

        Collections.reverse(result);

        return result;
    }

    // -------------------- Delete-first ordering --------------------

    static List<Op> normalize(List<Op> ops) {

        List<Op> result = new ArrayList<>();

        int i = 0;

        while (i < ops.size()) {

            if (ops.get(i).type == 'K') {
                result.add(ops.get(i));
                i++;
                continue;
            }

            // One change block
            List<Op> deletes = new ArrayList<>();
            List<Op> inserts = new ArrayList<>();

            while (i < ops.size() && ops.get(i).type != 'K') {

                if (ops.get(i).type == 'D') {
                    deletes.add(ops.get(i));
                } else {
                    inserts.add(ops.get(i));
                }

                i++;
            }

            // Assignment requires deletes first
            result.addAll(deletes);
            result.addAll(inserts);
        }

        return result;
    }

    // -------------------- Part A --------------------

    static void printLines(
            List<byte[]> A,
            List<byte[]> B,
            List<Op> originalOps) throws IOException {

        List<Op> ops = normalize(originalOps);

        BufferedOutputStream out =
                new BufferedOutputStream(System.out);

        for (Op op : ops) {

            if (op.type == 'K') {

                out.write(' ');
                out.write(A.get(op.aIndex));
                out.write('\n');

            } else if (op.type == 'D') {

                out.write('-');
                out.write(A.get(op.aIndex));
                out.write('\n');

            } else {

                out.write('+');
                out.write(B.get(op.bIndex));
                out.write('\n');
            }
        }

        out.flush();
    }

    // ============================================================
    // PART B - CHARACTER HIGHLIGHTING
    // ============================================================

    // Myers for integer arrays.
    // We use integer arrays because Unicode code points can be > 65535.

    static class CharOp {
        char type;
        int aIndex;
        int bIndex;

        CharOp(char type, int aIndex, int bIndex) {
            this.type = type;
            this.aIndex = aIndex;
            this.bIndex = bIndex;
        }
    }

    static List<CharOp> myersChars(int[] A, int[] B) {

        int N = A.length;
        int M = B.length;

        int maxD = N + M;
        int offset = maxD + 1;

        int[] V = new int[2 * maxD + 3];

        List<int[]> trace = new ArrayList<>();

        int finalD = 0;
        boolean finished = false;

        for (int d = 0; d <= maxD; d++) {

            trace.add(V.clone());

            for (int k = -d; k <= d; k += 2) {

                int index = offset + k;

                int x;

                if (k == -d ||
                    (k != d && V[index - 1] < V[index + 1])) {

                    x = V[index + 1];

                } else {

                    x = V[index - 1] + 1;
                }

                int y = x - k;

                while (x < N &&
                       y < M &&
                       A[x] == B[y]) {

                    x++;
                    y++;
                }

                V[index] = x;

                if (x >= N && y >= M) {
                    finalD = d;
                    finished = true;
                    break;
                }
            }

            if (finished) {
                break;
            }
        }

        List<CharOp> result = new ArrayList<>();

        int x = N;
        int y = M;

        for (int d = finalD; d > 0; d--) {

            int[] oldV = trace.get(d);

            int k = x - y;

            int previousK;

            if (k == -d ||
                (k != d &&
                 oldV[offset + k - 1] <
                 oldV[offset + k + 1])) {

                previousK = k + 1;

            } else {

                previousK = k - 1;
            }

            int previousX = oldV[offset + previousK];
            int previousY = previousX - previousK;

            while (x > previousX && y > previousY) {

                result.add(
                    new CharOp('K', x - 1, y - 1)
                );

                x--;
                y--;
            }

            if (x == previousX + 1) {

                result.add(
                    new CharOp('D', x - 1, -1)
                );

                x--;

            } else {

                result.add(
                    new CharOp('I', -1, y - 1)
                );

                y--;
            }
        }

        while (x > 0 && y > 0) {

            result.add(
                new CharOp('K', x - 1, y - 1)
            );

            x--;
            y--;
        }

        while (x > 0) {

            result.add(
                new CharOp('D', x - 1, -1)
            );

            x--;
        }

        while (y > 0) {

            result.add(
                new CharOp('I', -1, y - 1)
            );

            y--;
        }

        Collections.reverse(result);

        return result;
    }

    // -------------------- Character ranges --------------------

    static List<int[]> getChangedRanges(
            int[] oldChars,
            int[] newChars) {

        List<CharOp> ops = myersChars(oldChars, newChars);

        List<Integer> oldPositions = new ArrayList<>();
        List<Integer> newPositions = new ArrayList<>();

        for (CharOp op : ops) {

            if (op.type == 'D') {
                oldPositions.add(op.aIndex);

            } else if (op.type == 'I') {
                newPositions.add(op.bIndex);
            }
        }

        List<int[]> ranges = new ArrayList<>();

        // Old ranges
        List<int[]> oldRanges = makeRanges(oldPositions);

        // New ranges
        List<int[]> newRanges = makeRanges(newPositions);

        int max = Math.max(oldRanges.size(), newRanges.size());

        for (int i = 0; i < max; i++) {

            int oldStart = -1;
            int oldEnd = -1;

            int newStart = -1;
            int newEnd = -1;

            if (i < oldRanges.size()) {
                oldStart = oldRanges.get(i)[0];
                oldEnd = oldRanges.get(i)[1];
            }

            if (i < newRanges.size()) {
                newStart = newRanges.get(i)[0];
                newEnd = newRanges.get(i)[1];
            }

            ranges.add(new int[]{
                oldStart, oldEnd, newStart, newEnd
            });
        }

        return ranges;
    }

    static List<int[]> makeRanges(List<Integer> positions) {

        List<int[]> ranges = new ArrayList<>();

        if (positions.isEmpty()) {
            return ranges;
        }

        int start = positions.get(0);
        int previous = start;

        for (int i = 1; i < positions.size(); i++) {

            int current = positions.get(i);

            if (current == previous + 1) {

                previous = current;

            } else {

                ranges.add(
                    new int[]{start, previous + 1}
                );

                start = current;
                previous = current;
            }
        }

        ranges.add(
            new int[]{start, previous + 1}
        );

        return ranges;
    }

    static String rangeString(
            List<int[]> ranges,
            boolean oldSide) {

        StringBuilder sb = new StringBuilder();

        boolean first = true;

        for (int[] r : ranges) {

            int start;
            int end;

            if (oldSide) {

                start = r[0];
                end = r[1];

            } else {

                start = r[2];
                end = r[3];
            }

            if (start == -1) {
                continue;
            }

            if (!first) {
                sb.append(",");
            }

            sb.append(start);
            sb.append("-");
            sb.append(end);

            first = false;
        }

        if (first) {
            return ".";
        }

        return sb.toString();
    }

    // -------------------- Part B --------------------

    static void printHighlight(
            List<byte[]> A,
            List<byte[]> B,
            List<Op> originalOps) throws IOException {

        List<Op> ops = normalize(originalOps);

        BufferedOutputStream out =
                new BufferedOutputStream(System.out);

        int i = 0;

        while (i < ops.size()) {

            Op op = ops.get(i);

            if (op.type == 'K') {

                out.write(' ');
                out.write(A.get(op.aIndex));
                out.write('\n');

                i++;
                continue;
            }

            // Collect one change block
            List<Op> deletes = new ArrayList<>();
            List<Op> inserts = new ArrayList<>();

            while (i < ops.size() && ops.get(i).type != 'K') {

                if (ops.get(i).type == 'D') {
                    deletes.add(ops.get(i));
                } else {
                    inserts.add(ops.get(i));
                }

                i++;
            }

            int pairs = Math.min(
                    deletes.size(),
                    inserts.size()
            );

            // Print all deletes
            for (Op d : deletes) {

                out.write('-');
                out.write(A.get(d.aIndex));
                out.write('\n');
            }

            // Print inserts + ? lines
            for (int j = 0; j < inserts.size(); j++) {

                Op ins = inserts.get(j);

                out.write('+');
                out.write(B.get(ins.bIndex));
                out.write('\n');

                if (j < pairs) {

                    Op del = deletes.get(j);

                    String oldText =
                            new String(
                                A.get(del.aIndex),
                                StandardCharsets.UTF_8
                            );

                    String newText =
                            new String(
                                B.get(ins.bIndex),
                                StandardCharsets.UTF_8
                            );

                    int[] oldChars =
                            oldText.codePoints().toArray();

                    int[] newChars =
                            newText.codePoints().toArray();

                    List<int[]> ranges =
                            getChangedRanges(
                                oldChars,
                                newChars
                            );

                    List<int[]> oldRanges =
                            new ArrayList<>();

                    List<int[]> newRanges =
                            new ArrayList<>();

                    for (int[] r : ranges) {

                        if (r[0] != -1) {
                            oldRanges.add(
                                new int[]{r[0], r[1]}
                            );
                        }

                        if (r[2] != -1) {
                            newRanges.add(
                                new int[]{r[2], r[3]}
                            );
                        }
                    }

                    String oldPart =
                            rangeString(
                                ranges,
                                true
                            );

                    String newPart =
                            rangeString(
                                ranges,
                                false
                            );

                    String question =
                            "? " + oldPart +
                            " | " + newPart + "\n";

                    out.write(
                        question.getBytes(
                            StandardCharsets.UTF_8
                        )
                    );
                }
            }
        }

        out.flush();
    }

    // -------------------- Main --------------------

    public static void main(String[] args) {

        boolean known =
                args.length == 3 &&
                (args[0].equals("lines") ||
                 args[0].equals("highlight"));

        if (!known) {

            System.err.println(
                "usage: Main lines|highlight A_PATH B_PATH"
            );

            System.exit(2);
        }

        String command = args[0];
        String aPath = args[1];
        String bPath = args[2];

        List<byte[]> A;
        List<byte[]> B;

        try {

            A = getLines(readFile(aPath));
            B = getLines(readFile(bPath));

        } catch (IOException e) {

            System.err.println(
                "Error reading file"
            );

            System.exit(2);
            return;
        }

        List<Op> ops = myers(A, B);

        try {

            if (command.equals("lines")) {

                printLines(A, B, ops);

            } else {

                printHighlight(A, B, ops);
            }

        } catch (IOException e) {

            System.err.println(
                "Error writing output"
            );

            System.exit(2);
        }
    }
}