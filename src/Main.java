import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Main {

    static final byte KEEP = 0;
    static final byte DEL = 1;
    static final byte INS = 2;

    static class Diff {
        final int[] a;
        final int[] b;
        final int[] vf;
        final int[] vb;
        final int off;

        byte[] type;
        int[] opA;
        int[] opB;
        int count = 0;

        int msx, msy, msu, msv;

        Diff(int[] a, int[] b) {
            this.a = a;
            this.b = b;
            int size = a.length + b.length;
            int half = (size + 1) / 2 + 1;
            off = half + 1;
            vf = new int[2 * half + 3];
            vb = new int[2 * half + 3];
            type = new byte[size];
            opA = new int[size];
            opB = new int[size];
        }

        void emit(byte t, int ai, int bi) {
            type[count] = t;
            opA[count] = ai;
            opB[count] = bi;
            count++;
        }

        void run() {
            solve(0, a.length, 0, b.length);
        }

        void solve(int aLo, int aHi, int bLo, int bHi) {
            while (aLo < aHi && bLo < bHi && a[aLo] == b[bLo]) {
                emit(KEEP, aLo, bLo);
                aLo++;
                bLo++;
            }

            int sufA = aHi;
            int sufB = bHi;
            while (aLo < sufA && bLo < sufB && a[sufA - 1] == b[sufB - 1]) {
                sufA--;
                sufB--;
            }

            if (aLo == sufA) {
                for (int j = bLo; j < sufB; j++) {
                    emit(INS, -1, j);
                }
            } else if (bLo == sufB) {
                for (int i = aLo; i < sufA; i++) {
                    emit(DEL, i, -1);
                }
            } else {
                middleSnake(aLo, sufA, bLo, sufB);
                int x = msx, y = msy, u = msu, v = msv;
                solve(aLo, aLo + x, bLo, bLo + y);
                for (int t = 0; t < u - x; t++) {
                    emit(KEEP, aLo + x + t, bLo + y + t);
                }

                solve(aLo + u, sufA, bLo + v, sufB);
            }

            for (int t = 0; t < aHi - sufA; t++) {
                emit(KEEP, sufA + t, sufB + t);
            }
        }

        void middleSnake(int aLo, int aHi, int bLo, int bHi) {

            int N = aHi - aLo;
            int M = bHi - bLo;
            int delta = N - M;
            boolean odd = (delta & 1) != 0;
            int max = (N + M + 1) / 2;

            vf[off + 1] = 0;
            vb[off + 1] = 0;

            for (int d = 0; d <= max; d++) {
                for (int k = -d; k <= d; k += 2) {
                    int idx = off + k;
                    int x;
                    if (k == -d || (k != d && vf[idx - 1] < vf[idx + 1])) {
                        x = vf[idx + 1];
                    } else {
                        x = vf[idx - 1] + 1;
                    }

                    int y = x - k;
                    int sx = x;
                    int sy = y;

                    while (x < N && y < M && a[aLo + x] == b[bLo + y]) {
                        x++;
                        y++;
                    }

                    vf[idx] = x;

                    if (odd && k >= delta - (d - 1) && k <= delta + (d - 1) && x + vb[off + delta - k] >= N) {
                        msx = sx;
                        msy = sy;
                        msu = x;
                        msv = y;
                        return;
                    }
                }

                for (int k = -d; k <= d; k += 2) {

                    int idx = off + k;
                    int x;

                    if (k == -d || (k != d && vb[idx - 1] < vb[idx + 1])) {
                        x = vb[idx + 1];
                    } else {
                        x = vb[idx - 1] + 1;
                    }

                    int y = x - k;
                    int sx = x;
                    int sy = y;

                    while (x < N && y < M && a[aHi - 1 - x] == b[bHi - 1 - y]) {
                        x++;
                        y++;
                    }

                    vb[idx] = x;

                    if (!odd && delta - k >= -d && delta - k <= d && x + vf[off + delta - k] >= N) {
                        msx = N - x;
                        msy = M - y;
                        msu = N - sx;
                        msv = M - sy;
                        return;
                    }
                }
            }
            throw new IllegalStateException("no middle snake");
        }
    }

    static class Lines {
        byte[] data;
        int[] start;
        int[] end;
        int n;
        int[] ids;
    }

    static Lines getLines(byte[] data) {
        int count = 0;
        for (byte x : data) {
            if (x == '\n') {
                count++;
            }
        }
        if (data.length > 0 && data[data.length - 1] != '\n') {
            count++;
        }

        Lines L = new Lines();
        L.data = data;
        L.n = count;
        L.start = new int[count];
        L.end = new int[count];
        L.ids = new int[count];

        int s = 0;
        int c = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == '\n') {
                L.start[c] = s;
                L.end[c] = i;
                c++;
                s = i + 1;
            }
        }
        if (s < data.length) {
            L.start[c] = s;
            L.end[c] = data.length;
        }
        return L;
    }

    static void assignIds(Lines A, Lines B) {

        int total = A.n + B.n;
        int cap = 16;
        while (cap < 2 * total) {
            cap <<= 1;
        }
        int mask = cap - 1;

        int[] table = new int[cap];
        Lines[] repFile = new Lines[total];
        int[] repLine = new int[total];
        int nextId = 0;

        Lines[] files = {A, B};

        for (Lines f : files) {
            for (int i = 0; i < f.n; i++) {

                int s = f.start[i];
                int e = f.end[i];

                int h = 1;
                for (int p = s; p < e; p++) {
                    h = 31 * h + f.data[p];
                }
                h ^= (h >>> 16);
                h *= 0x85ebca6b;
                h ^= (h >>> 13);

                int pos = h & mask;
                int id = -1;

                while (table[pos] != 0) {
                    int cand = table[pos] - 1;
                    Lines cf = repFile[cand];
                    int cl = repLine[cand];
                    if (Arrays.equals(cf.data, cf.start[cl], cf.end[cl], f.data, s, e)) {
                        id = cand;
                        break;
                    }
                    pos = (pos + 1) & mask;
                }

                if (id == -1) {
                    id = nextId++;
                    table[pos] = id + 1;
                    repFile[id] = f;
                    repLine[id] = i;
                }

                f.ids[i] = id;
            }
        }
    }

    // Part A

    static void writeLine(OutputStream out, int prefix, Lines F, int line) throws IOException {
        out.write(prefix);
        out.write(F.data, F.start[line], F.end[line] - F.start[line]);
        out.write('\n');
    }

    static void printLines(Lines A, Lines B, Diff d, OutputStream out) throws IOException {

        int i = 0;
        while (i < d.count) {

            if (d.type[i] == KEEP) {
                writeLine(out, ' ', A, d.opA[i]);
                i++;
                continue;
            }

            int j = i;
            while (j < d.count && d.type[j] != KEEP) {
                j++;
            }

            for (int t = i; t < j; t++) {
                if (d.type[t] == DEL) {
                    writeLine(out, '-', A, d.opA[t]);
                }
            }
            for (int t = i; t < j; t++) {
                if (d.type[t] == INS) {
                    writeLine(out, '+', B, d.opB[t]);
                }
            }
            i = j;
        }
    }

    // Part B

    static int[] codePoints(Lines F, int line) {
        String s = new String(F.data, F.start[line], F.end[line] - F.start[line], StandardCharsets.UTF_8);
        return s.codePoints().toArray();
    }

    static String ranges(int[] pos, int n) {
        if (n == 0) {
            return ".";
        }
        StringBuilder sb = new StringBuilder();
        int start = pos[0];
        int prev = start;
        for (int i = 1; i < n; i++) {
            if (pos[i] == prev + 1) {
                prev = pos[i];
            } else {
                if (sb.length() > 0) sb.append(',');
                sb.append(start).append('-').append(prev + 1);
                start = pos[i];
                prev = start;
            }
        }
        if (sb.length() > 0) sb.append(',');
        sb.append(start).append('-').append(prev + 1);
        return sb.toString();
    }

    static String question(int[] oldChars, int[] newChars) {
        Diff cd = new Diff(oldChars, newChars);
        cd.run();

        int[] oldPos = new int[cd.count];
        int[] newPos = new int[cd.count];
        int no = 0;
        int nn = 0;
        for (int i = 0; i < cd.count; i++) {
            if (cd.type[i] == DEL) {
                oldPos[no++] = cd.opA[i];
            } else if (cd.type[i] == INS) {
                newPos[nn++] = cd.opB[i];
            }
        }
        return "? " + ranges(oldPos, no) + " | " + ranges(newPos, nn) + "\n";
    }

    static void printHighlight(Lines A, Lines B, Diff d, OutputStream out) throws IOException {

        int i = 0;
        while (i < d.count) {

            if (d.type[i] == KEEP) {
                writeLine(out, ' ', A, d.opA[i]);
                i++;
                continue;
            }

            int j = i;
            while (j < d.count && d.type[j] != KEEP) {
                j++;
            }

            int nd = 0;
            int ni = 0;
            for (int t = i; t < j; t++) {
                if (d.type[t] == DEL) nd++; else ni++;
            }
            int[] dels = new int[nd];
            int[] inss = new int[ni];
            nd = 0;
            ni = 0;
            for (int t = i; t < j; t++) {
                if (d.type[t] == DEL) dels[nd++] = d.opA[t];
                else inss[ni++] = d.opB[t];
            }

            for (int t = 0; t < nd; t++) {
                writeLine(out, '-', A, dels[t]);
            }

            int pairs = Math.min(nd, ni);

            for (int t = 0; t < ni; t++) {
                writeLine(out, '+', B, inss[t]);

                if (t < pairs) {
                    String q = question(codePoints(A, dels[t]), codePoints(B, inss[t]));
                    out.write(q.getBytes(StandardCharsets.UTF_8));
                }
            }
            i = j;
        }
    }

    public static void main(String[] args) {

        boolean known = args.length == 3 && (args[0].equals("lines") || args[0].equals("highlight"));

        if (!known) {
            System.err.println("usage: Main lines|highlight A_PATH B_PATH");
            System.exit(2);
        }

        Lines A;
        Lines B;

        try {
            A = getLines(Files.readAllBytes(Paths.get(args[1])));
            B = getLines(Files.readAllBytes(Paths.get(args[2])));
        } catch (IOException | RuntimeException e) {
            System.err.println("Error reading file");
            System.exit(2);
            return;
        }

        assignIds(A, B);

        Diff d = new Diff(A.ids, B.ids);
        d.run();

        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(FileDescriptor.out), 1 << 16);

            if (args[0].equals("lines")) {
                printLines(A, B, d, out);
            } else {
                printHighlight(A, B, d, out);
            }
            out.flush();
        } catch (IOException e) {
            System.err.println("Error writing output");
            System.exit(2);
        }
    }
}
