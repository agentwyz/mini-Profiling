package org.example;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;



public class Store {
    private final int MAX_FLAMEGRAPH_DEPTH = 100; //设置整个火焰图最大的深度

    private final Map<String, Long> methodOnTopSampleCount = new HashMap<>();
    private final Map<String, Long> methodSampleCount = new HashMap<>();


    private final Optional<Path> flamePath;

    public Store(Optional<Path> flamePath) {
        this.flamePath = flamePath;
    }

    private String flattenStackTraceElement(StackTraceElement stackTraceElement) {
        return (stackTraceElement.getMethodName() + "." + stackTraceElement.getMethodName()).intern();
    }


    private void updateMethodTables(String method, boolean onTop) {
        methodSampleCount.put(method, methodSampleCount.getOrDefault(method, 0L) + 1);

        if (onTop) {
            methodOnTopSampleCount.put(method, methodOnTopSampleCount.getOrDefault(method, 0L) + 1);
        }
    }

    private void updateMethodTables(List<String> trace) {
        for (int i = 0; i < trace.size(); i++) {
            String method = trace.get(i);
            updateMethodTables(method, i == 0);
        }
    }


    private final Node rootNode = new Node("root");

    void addSample(StackTraceElement[] stackTraceElements) {

        //首先获取所有的栈名称
        List<String> trace = Stream.of(stackTraceElements).map(this::flattenStackTraceElement).toList();

        //然后更新我们的列表
        updateMethodTables(trace);

        if (flamePath.isPresent()) {
            rootNode.addTrace(trace);
        }

        totalSampleCount++;

    }

    private class MethodTableEntry {
        String method;
        long sampleCount;
        long onTopSampleCount;

        public MethodTableEntry(String method, long sampleCount, long onTopSampleCount) {
            this.method = method;
            this.sampleCount = sampleCount;
            this.onTopSampleCount = onTopSampleCount;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public long getSampleCount() {
            return sampleCount;
        }

        public void setSampleCount(long sampleCount) {
            this.sampleCount = sampleCount;
        }

        public long getOnTopSampleCount() {
            return onTopSampleCount;
        }

        public void setOnTopSampleCount(long onTopSampleCount) {
            this.onTopSampleCount = onTopSampleCount;
        }
    }

    private void printMethodTable(PrintStream s, List<MethodTableEntry> sortedEntries) {
        s.printf("===== method table ======%n");
        s.printf("Total samples: %d%n", totalSampleCount);
        s.printf("%-60s %10s %10s %10s %10s%n", "Method", "Samples", "Percentage", "On top", "Percentage");
        for (MethodTableEntry entry : sortedEntries) {
            String method = entry.method.substring(0, Math.min(entry.method.length(), 60));
            s.printf("%-60s %10d %10.2f %10d %10.2f%n", method, entry.getSampleCount(),
                    entry.getSampleCount() / (double) totalSampleCount * 100, entry.getOnTopSampleCount(),
                    entry.getOnTopSampleCount() / (double) totalSampleCount * 100);
        }
    }

    public void printMethodTable() {
        List<MethodTableEntry> methodTable =
                methodSampleCount.entrySet().stream().map(entry -> new MethodTableEntry(entry.getKey(),
                                entry.getValue(), methodOnTopSampleCount.getOrDefault(entry.getKey(), 0L)))
                        .sorted((a, b) -> Long.compare(b.sampleCount, a.sampleCount)).toList();
        printMethodTable(System.out, methodTable);
    }

    public void storeFlameGraphIfNeeded() {
        flamePath.ifPresent(path -> {
            try (OutputStream os = new BufferedOutputStream(java.nio.file.Files.newOutputStream(path))) {
                PrintStream s = new PrintStream(os);
                rootNode.writeAsHTML(s, MAX_FLAMEGRAPH_DEPTH);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }




    private long totalSampleCount = 0;

    private static class Node {
        private final String method;
        private long samples = 0;
        private final Map<String, Node> children = new HashMap<>();

        public Node(String method) {
            this.method = method;
        }

        private void addTrace(List<String> trace, int end) {
            samples++;
            if (end > 0) {
                getChild(trace.get(end)).addTrace(trace, end - 1);
            }
        }

        public void addTrace(List<String> trace) {
            addTrace(trace, trace.size()-1);
        }



        public Node getChild(String method) {
            return children.computeIfAbsent(method, Node::new);
        }

        private void writeAsJson(PrintStream s, int maxDepth) {
            s.printf("{ \"name\": \"%s\", \"value\": %d, \"children\": [", method, samples);

            if (maxDepth > 1) {
                for (Node child: children.values()) {
                    child.writeAsJson(s, maxDepth-1);
                    s.println(",");
                }
            }

            s.print("]}");
        }

        public void writeAsHTML(PrintStream s, int maxDepth) {
            s.print("""
                    <head>
                      <link rel="stylesheet" type="text/css" href="https://cdn.jsdelivr.net/npm/d3-flame-graph@4.1.3/dist/d3-flamegraph.css">
                      <link rel="stylesheet" type="text/css" href="misc/d3-flamegraph.css">
                    </head>
                    <body>
                      <div id="chart"></div>
                      <script type="text/javascript" src="https://d3js.org/d3.v7.js"></script>
                      <script type="text/javascript" src="misc/d3.v7.js"></script>
                      <script type="text/javascript" src="https://cdn.jsdelivr.net/npm/d3-flame-graph@4.1.3/dist/d3-flamegraph.js"></script>
                      <script type="text/javascript" src="misc/d3-flamegraph.js"></script>
                      <script type="text/javascript">
                      var chart = flamegraph().width(window.innerWidth);
                      d3.select("#chart").datum(""");
            writeAsJson(s, maxDepth);
            s.print("""
                    ).call(chart);
                      window.onresize = () => chart.width(window.innerWidth);
                      </script>
                    </body>
                    """);
        }


    }



}
