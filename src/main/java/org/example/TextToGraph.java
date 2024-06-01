package org.example;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSinkDOT;

import java.util.*;
import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.FileWriter;
import java.util.Arrays;


public class TextToGraph {
    // 邻接矩阵限制
    public static int maxValue = 10000;
    public static String pathStr;

    public static void main(String[] args){
        System.out.print("请输入想要输入的文本数据：");
        Scanner scanner = new Scanner(System.in);
        String baseFile = scanner.nextLine();
        String[] words = FileProcess(baseFile);
        // 调用createGraph方法创建有向图
        Map<String, Map<String, Integer>> graph = createGraph(words);
        showDirectedGraph(graph);
        Scanner scanner1 = new Scanner(System.in);
        int choice = -1;

        while (choice != 0) {
            System.out.println("-----------功能菜单-----------");
            System.out.println("1. 查询桥接词");
            System.out.println("2. 根据 bridge word 生成新文本");
            System.out.println("3. 计算两个单词之间的最短路径");
            System.out.println("4. 随机游走");
            System.out.println("0. 退出");
            System.out.print("请输入想要执行的功能：");

            choice = scanner1.nextInt();
            System.out.print("");
            switch (choice) {
                case 1 -> {
                    // 执行功能1 查询桥接词
                    System.out.print("请输入想要查询的两个单词：");
                    Scanner scanner2 = new Scanner(System.in);
                    System.out.print("");
                    String word1 = scanner2.next();
                    String word2 = scanner2.next();
                    String[] BridgeWords = queryBridgeWords(word1, word2, graph);
                    if (BridgeWords.length > 1) {
                        showDirectedGraph(graph, BridgeWords, word1);
                        System.out.print(word1 + " 与 " + word2 + " 之间桥接词是: " + word1);
                        for (String bridgeWord : BridgeWords) {
                            System.out.print(" -> " + bridgeWord);
                        }
                    } else if (BridgeWords.length == 1) {
                        System.out.println(word1 + "与" + word2 + "之间不存在桥接词");
                    }
                }
                case 2 -> {
                    // 执行功能2 根据 bridge word 生成新文本
                    String newtext = generateNewText(graph);
                    System.out.println("输出的新文本为: " + newtext);
                }
                case 3 -> {
                    // 执行功能3 最短路径
                    System.out.print("输入你想要的单词： ");
                    Scanner scanner3 = new Scanner(System.in);
                    String input = scanner3.nextLine();
                    String[] arg = input.split(" ");
                    if (arg.length == 1) {
                        String word3 = arg[0];
                        System.out.print("");
                        if (!graph.containsKey(word3)) {
                            System.out.println("这不是图中的点");
                            break;
                        }
                        String[] resultStr = calcShortestPath(word3, graph);
                        System.out.println("所有路径为：");
                        int count = 0, pathNum = 1;
                        Map<String, Integer> testMap = graph.get(word3);
                        for (; count < resultStr.length; count++) {
                            String[] test = resultStr[count].split("->");
                            if (testMap.containsKey(test[1])) {
                                System.out.println("第 " + pathNum + " 条路径为: " + resultStr[count]);
                                pathNum++;
                            }
                        }
                        if (pathNum == 1) {
                            System.out.println("这是一个终止点");
                        }
                    }
                    if (arg.length == 2) {
                        String word3 = arg[0];
                        String word4 = arg[1];
                        System.out.print("");
                        if (!graph.containsKey(word3) || !graph.containsKey(word4)) {
                            System.out.println("其中存在不属于图的点");
                            break;
                        }
                        String resultStr = calcShortestPath(word3, word4, graph);
                        String[] test = resultStr.split("->");
                        showDirectedGraph(graph, test);
                        Map<String, Integer> testMap = graph.get(test[0]);
                        if (!testMap.containsKey(test[1])) {
                            System.out.println("这条路径不存在");
                            break;
                        }
                        System.out.println("最短路径为： " + resultStr);
                    }
                }
                case 4 -> {
                    // 执行功能4 随机游走
                    AtomicBoolean stopRequested = new AtomicBoolean(false); // 标志变量
                    AtomicBoolean stopRequested1 = new AtomicBoolean(false);
                    Thread thread = new Thread(() -> {
                        String path = randomWalk(graph, stopRequested);
                        pathStr = path;
                        System.out.println("随机游走的结果为：" + path);
                        stopRequested1.set(true);  // 随机游走结束后设置标志变量为 true，退出命令行等待的循环
                    });
                    thread.start();

                    // 在命令行中等待用户输入来停止遍历
                    Scanner scanner4 = new Scanner(System.in);
                    while (!stopRequested1.get()) {
                        String input1 = scanner4.nextLine().trim();
                        if (input1.equalsIgnoreCase("stop")) {
                            thread.interrupt(); // 发送中断信号
                            stopRequested.set(true);
                        }
                    }

                    if (pathStr.endsWith(" (dead node)")) {
                        pathStr = pathStr.substring(0, pathStr.lastIndexOf(" (dead node)"));
                    }
                    else if (pathStr.endsWith("(repeat)")) {
                        pathStr = pathStr.substring(0, pathStr.lastIndexOf("(repeat)"));
                    }
                    String[] test = pathStr.split(" ");
                    showDirectedGraph_randomWalk(graph, test);
                }
                case 0 ->
                    // 退出程序
                        System.out.println("退出程序");
                default -> System.out.println("无效的选择，请重新输入");
            }
            System.out.println();
        }
        scanner.close();
    }


    /**
     * 绘制图像
     * @author cpx
     * @param words 分词处理后的源文件
     * @return Map 图结果
     */
    public static Map<String,  Map<String, Integer>> createGraph(String[] words){
        Map<String, Map<String, Integer>> graph = new HashMap<>();

        for (int i = 0; i < words.length - 1; i++) {
            String source = words[i];
            String target = words[i + 1];

            if (!source.equals(target)) {
                Map<String, Integer> neighbors = graph.getOrDefault(source, new HashMap<>());
                int weight = neighbors.getOrDefault(target, 0);
                neighbors.put(target, weight + 1);
                graph.put(source, neighbors);
            }
        }

        // 手动添加最后一个单词
        if (words.length > 0) {
            String lastWord = words[words.length - 1];
            Map<String, Integer> neighbors = graph.getOrDefault(lastWord, new HashMap<>());
            graph.put(lastWord, neighbors);
        }
        return graph;
    }


    /**
     * 文件分词处理函数
     * @author wpy
     * @param filename 预处理文件名
     * @return string 得到的字符串结果
     */
    public static String[] FileProcess(String filename){
        File inputFile = new File(filename);
        String wordsStr = "";
        String[] fileWords;
        Scanner read;
        try {
            read = new Scanner(inputFile);
            while(read.hasNextLine()){
                String str = read.nextLine();
                wordsStr = wordsStr.concat((str.replaceAll("[^a-zA-Z]", " ").toLowerCase()) + " ");
            }
            fileWords = wordsStr.split("\\s+");
            read.close();
            System.out.println("文本数据为: ");
            for (String fileWord : fileWords) {
                System.out.print(fileWord + " ");
            }
            System.out.println();
            return fileWords;
        }
        catch (FileNotFoundException e){
            e.printStackTrace();
            return new String[0];
        }
    }


    /**
     * 图像生成函数
     * @author cpx
     * @param graph 图结构
     */
    public static void showDirectedGraph(Map<String,  Map<String, Integer>> graph){
        //屏幕打印
        System.out.println("各边与权值为: ");
        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            String source = entry.getKey();
            Map<String, Integer> neighbors = entry.getValue();

            for (Map.Entry<String, Integer> neighbor : neighbors.entrySet()) {
                String target = neighbor.getKey();
                int weight = neighbor.getValue();
                System.out.println(source + " -> " + target + ", weight: " + weight);
            }
        }

        //文件保存并展示
        // 创建GraphStream图对象
        Graph directedGraph = new SingleGraph("Directed Graph");

        // 遍历有向图中的节点和边
        for (String source : graph.keySet()) {
            // 添加节点
            if(directedGraph.getNode(source) == null){
                directedGraph.addNode(source);
            }

            // 添加边
            Map<String, Integer> neighbors = graph.get(source);
            for (String target : neighbors.keySet()) {
                int weight = neighbors.get(target);
                if(directedGraph.getNode(target) == null){
                    directedGraph.addNode(target);
                }
                Edge edge = directedGraph.addEdge(source + "_" + target, source, target, true);
                edge.setAttribute("label", String.valueOf(weight));
            }
        }

        // 设置节点和边的样式（可选）
        directedGraph.setAttribute("ui.stylesheet", "node { size: 20px; text-size: 12px; } edge { text-size: 10px; }");

        // 保存有向图为图片文件
        try {
            Thread.sleep(1000); // 等待图形界面加载完成

            // 使用Graphviz进行布局
            FileSinkDOT fileSinkDOT = new FileSinkDOT();
            fileSinkDOT.setDirected(true);
            fileSinkDOT.writeAll(directedGraph, "directed_graph.dot"); // 将有向图保存为DOT文件

            String dotFilePath = "directed_graph.dot";
            String outputFilePath = "directed_graph.png";
            String command = "dot -Tpng " + dotFilePath + " -o " + outputFilePath;

            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            processBuilder.directory(new File(System.getProperty("user.dir")));
            Process process = processBuilder.start();
            process.waitFor(); // 等待命令执行完成

            System.out.println("有向图已保存到：" + outputFilePath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 图结构展示，重载方法
     * @author cpx
     * @param graph 图结构
     * @param words 桥接词路径
     * @param start 起始单词
     */
    public static void showDirectedGraph(Map<String,  Map<String, Integer>> graph, String[] words, String start){
        //文件保存并展示
        // 创建GraphStream图对象
        Graph directedGraph = new SingleGraph("Directed Graph");

        // 遍历有向图中的节点和边
        for (String source : graph.keySet()) {
            // 添加节点
            if(directedGraph.getNode(source) == null){
                directedGraph.addNode(source);
            }
            // 添加边
            Map<String, Integer> neighbors = graph.get(source);
            for (String target : neighbors.keySet()) {
                int weight = neighbors.get(target);
                if(directedGraph.getNode(target) == null){
                    directedGraph.addNode(target);
                }
                Edge edge = directedGraph.addEdge(source + "_" + target, source, target, true);
                if (source.equals(start) && target.equals(words[0])){
                    edge.setAttribute("color", "green");
                }
                if (Arrays.asList(words).contains(source) && Arrays.asList(words).contains(target)){
                    edge.setAttribute("color", "green");
                }

                edge.setAttribute("label", String.valueOf(weight));
            }
        }

        // 设置节点和边的样式（可选）
        directedGraph.setAttribute("ui.stylesheet", "node { size: 20px; text-size: 12px; fill-color: black; } edge { text-size: 10px; fill-color: black; } edge.highlighted { fill-color: green; }");

        // 保存有向图为图片文件
        try {
            Thread.sleep(1000); // 等待图形界面加载完成
            //TimeUnit.SECONDS.sleep(1); // 等待图形界面加载完成

            // 使用Graphviz进行布局
            FileSinkDOT fileSinkDOT = new FileSinkDOT();
            fileSinkDOT.setDirected(true);
            fileSinkDOT.writeAll(directedGraph, "directed_graph_queryBridgeWords.dot"); // 将有向图保存为DOT文件

            String dotFilePath = "directed_graph_queryBridgeWords.dot";
            String outputFilePath = "directed_graph_queryBridgeWords.png";
            String command = "dot -Tpng " + dotFilePath + " -o " + outputFilePath;

            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            processBuilder.directory(new File(System.getProperty("user.dir")));
            Process process = processBuilder.start();
            process.waitFor(); // 等待命令执行完成

            System.out.println("有向图已保存到：" + outputFilePath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 图结构展示，重载方法
     * @author cpx
     * @param graph 图结构
     * @param words 最短路径
     */
    public static void showDirectedGraph(Map<String,  Map<String, Integer>> graph, String[] words){
        //文件保存并展示
        // 创建GraphStream图对象
        Graph directedGraph = new SingleGraph("Directed Graph");

        // 遍历有向图中的节点和边
        for (String source : graph.keySet()) {
            // 添加节点
            if(directedGraph.getNode(source) == null){
                directedGraph.addNode(source);
            }
            // 添加边
            Map<String, Integer> neighbors = graph.get(source);
            for (String target : neighbors.keySet()) {
                int weight = neighbors.get(target);
                if(directedGraph.getNode(target) == null){
                    directedGraph.addNode(target);
                }
                Edge edge = directedGraph.addEdge(source + "_" + target, source, target, true);
                if (Arrays.asList(words).contains(source) && Arrays.asList(words).contains(target)){
                    edge.setAttribute("color", "green");
                }

                edge.setAttribute("label", String.valueOf(weight));
            }
        }

        // 设置节点和边的样式（可选）
        directedGraph.setAttribute("ui.stylesheet", "node { size: 20px; text-size: 12px; fill-color: black; } edge { text-size: 10px; fill-color: black; } edge.highlighted { fill-color: green; }");

        // 保存有向图为图片文件
        try {
            Thread.sleep(1000); // 等待图形界面加载完成

            // 使用Graphviz进行布局
            FileSinkDOT fileSinkDOT = new FileSinkDOT();
            fileSinkDOT.setDirected(true);
            fileSinkDOT.writeAll(directedGraph, "directed_graph_calcShortestPath.dot"); // 将有向图保存为DOT文件

            String dotFilePath = "directed_graph_calcShortestPath.dot";
            String outputFilePath = "directed_graph_calcShortestPath.png";
            String command = "dot -Tpng " + dotFilePath + " -o " + outputFilePath;

            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            processBuilder.directory(new File(System.getProperty("user.dir")));
            Process process = processBuilder.start();
            process.waitFor(); // 等待命令执行完成

            System.out.println("有向图已保存到：" + outputFilePath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 图结构展示，重载方法
     * @author cpx
     * @param graph 图结构
     * @param words 随机游走路径
     */
    public static void showDirectedGraph_randomWalk(Map<String,  Map<String, Integer>> graph, String[] words){
        //文件保存并展示
        // 创建GraphStream图对象
        Graph directedGraph = new SingleGraph("Directed Graph");

        // 遍历有向图中的节点和边
        for (String source : graph.keySet()) {
            // 添加节点
            if(directedGraph.getNode(source) == null){
                directedGraph.addNode(source);
            }
            // 添加边
            Map<String, Integer> neighbors = graph.get(source);
            for (String target : neighbors.keySet()) {
                int weight = neighbors.get(target);
                if(directedGraph.getNode(target) == null){
                    directedGraph.addNode(target);
                }
                Edge edge = directedGraph.addEdge(source + "_" + target, source, target, true);
                if (Arrays.asList(words).contains(source) && Arrays.asList(words).contains(target)){
                    edge.setAttribute("color", "green");
                }

                edge.setAttribute("label", String.valueOf(weight));
            }
        }

        // 设置节点和边的样式（可选）
        directedGraph.setAttribute("ui.stylesheet", "node { size: 20px; text-size: 12px; fill-color: black; } edge { text-size: 10px; fill-color: black; } edge.highlighted { fill-color: green; }");

        // 保存有向图为图片文件
        try {
            Thread.sleep(1500); // 等待图形界面加载完成

            // 使用Graphviz进行布局
            FileSinkDOT fileSinkDOT = new FileSinkDOT();
            fileSinkDOT.setDirected(true);
            fileSinkDOT.writeAll(directedGraph, "directed_graph_randomWalk.dot"); // 将有向图保存为DOT文件

            String dotFilePath = "directed_graph_randomWalk.dot";
            String outputFilePath = "directed_graph_randomWalk.png";
            String command = "dot -Tpng " + dotFilePath + " -o " + outputFilePath;

            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            processBuilder.directory(new File(System.getProperty("user.dir")));
            Process process = processBuilder.start();
            process.waitFor(); // 等待命令执行完成

            System.out.println("有向图已保存到：" + outputFilePath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 查询桥接词功能
     * @author wpy
     * @param word1 桥接词 首词
     * @param word2 桥接词 末词
     * @param graph 图结构
     * @return 桥接词路劲
     */
    public static String[] queryBridgeWords(String word1, String word2, Map<String, Map<String, Integer>> graph){
        // 如果没有图或起始词/终止词不在图中，返回空数组
        if (graph == null || !graph.containsKey(word1) || !graph.containsKey(word2)) {
            System.out.println("你想要查找的单词不在图中");
            return new String[0];
        }

        List<String> path = new ArrayList<>();
        path.add(word1); // 将起始词添加到路径中
        Set<String> visited = new HashSet<>();
        visited.add(word1); // 标记起始词为已访问
        boolean found = dfs(word1, word2, graph, path, visited);
        // 移除word1
        if (found && path.size() > 1) {
            path.remove(0); // 移除word1
            return path.toArray(new String[0]);
        }
        System.out.println(word1 + "与" + word2 + "之间不存在桥接词");
        // 如果没有找到路径，返回空数组
        return new String[0];
    }


    /**
     * 查询桥接词，重载方法，其他功能调用时的设置
     * @author wpy
     * @param word1 桥接词 首词
     * @param word2 桥接词 末词
     * @param graph 图结构
     * @param check 标记位置
     * @return 与桥接词有关的语句
     */
    public static String[] queryBridgeWords(String word1, String word2, Map<String, Map<String, Integer>> graph,  int check){
        // 如果没有图或起始词/终止词不在图中，返回空数组
        if ((graph == null || !graph.containsKey(word1) || !graph.containsKey(word2)) && check==0) {
            System.out.println("你想要查找的单词不在图中");
            return new String[0];
        }

        List<String> path = new ArrayList<>();
        path.add(word1); // 将起始词添加到路径中
        Set<String> visited = new HashSet<>();
        visited.add(word1); // 标记起始词为已访问
        boolean found = dfs(word1, word2, graph, path, visited);
        // 移除word1
        if (found && path.size() > 1) {
            path.remove(0); // 移除word1
            return path.toArray(new String[0]);
        }
        System.out.println(word1 + "与" + word2 + "之间不存在桥接词");
        // 如果没有找到路径，返回空数组
        return new String[0];
    }


    /**
     * dfs搜索
     * @author wpy
     * @param current 路径首节点
     * @param target 路径目标
     * @param graph 图结构
     * @param path 当前路径数组
     * @param visited 是否访问过节点的字符数组
     * @return 判断是否有路径
     */
    public static boolean dfs(String current, String target, Map<String, Map<String, Integer>> graph, List<String> path, Set<String> visited){
        if (current.equals(target)) {
            return true; // 找到了路径
        }
        Map<String, Integer> neighbors = graph.get(current);
        if (neighbors != null) {
            for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
                String neighbor = entry.getKey();
                // 如果邻居未被访问过，则递归访问
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor); // 标记邻居为已访问
                    path.add(neighbor); // 将邻居添加到路径中

                    // 递归调用DFS
                    if (dfs(neighbor, target, graph, path, visited)) {
                        return true; // 找到了路径
                    }
                    // 回溯
                    path.remove(path.size() - 1); // 从路径中移除邻居
                }
            }
        }
        return false;
    }


    /**
     * 创建新文本
     * @author wpy
     * @param graph 图结构
     * @return 新文本字符串
     */
    public static String generateNewText(Map<String, Map<String, Integer>> graph){
        Scanner getNewStr = new Scanner(System.in);
        System.out.print("请输入新文本: ");
        String s2 = getNewStr.nextLine();
        System.out.print("");
        s2 = s2.replaceAll("[^a-zA-Z]", " ").toLowerCase();
        List<String> words = new ArrayList<>();
        for (String word : s2.split("\\s+")) {
            words.add(word);
        }
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<words.size()-1; i++){
            String currentWord = words.get(i);
            String nextWord = words.get(i + 1);
            String[] result = queryBridgeWords(currentWord, nextWord, graph, 1);
            sb.append(currentWord);
            sb.append(" ");
            if(result.length == 0|| result.length == 1){}
            else {
                Random ramdom = new Random();
                int num = ramdom.nextInt(0, result.length-1);
                sb.append(result[num]);
                sb.append(" ");
            }
        }
        sb.append(words.get(words.size() - 1));
        return sb.toString();
    }


    /**
     * 计算最短路径
     * @author wpy
     * @param word1 只有一个单词时的计算全部路径
     * @param graph 图结构
     * @return 全部的最短路径
     */
    public static String[] calcShortestPath(String word1, Map<String, Map<String, Integer>> graph){
        int index = 0;
        int matrixSize = graph.size();
        int[][] matrix = new int[matrixSize][matrixSize];
        // 将字符与唯一数字关联
        // 初始化邻接矩阵
        for(int i=0; i<matrixSize; i++){
            for(int j=0; j<matrixSize; j++){
                matrix[i][j] = maxValue;
            }
        }

        Map<String, Integer> stringToMap = new HashMap<>();
        for(String key : graph.keySet()){
            stringToMap.put(key, index);
            index++;
        }
        int start = stringToMap.get(word1);

        // 修改邻接矩阵值
        for(int i=0; i<matrixSize; i++){
            String key = null;
            for (Map.Entry<String, Integer> entry : stringToMap.entrySet()) {
                if (entry.getValue() == i) {
                    key = entry.getKey();
                    break;
                }
            }
            Map<String, Integer> innerMap = graph.get(key);
            for(Map.Entry<String, Integer> entry : innerMap.entrySet()){
                String innerStr = entry.getKey();
                if(innerStr == null){
                    break;
                }
                int value = entry.getValue();
                int innerStrToInt = stringToMap.get(innerStr);
                matrix[i][innerStrToInt] = value;
            }
        }
        int[] shortPath = new int[matrixSize];
        int[] visitedNode = new int[matrixSize];
        String[] path = new String[matrixSize];
        shortPath[start] = 0;
        visitedNode[start] = 1;
        for(int i=0; i<matrixSize; i++){
            String name = null;
            for (Map.Entry<String, Integer> entry : stringToMap.entrySet()) {
                if (entry.getValue() == i) {
                    name = entry.getKey();
                    break;
                }
            }
            path[i] = new String(word1 + "->" + name);
        }
        for(int i=0; i<matrixSize; i++){
            int min = maxValue;
            int node = -1;
            for(int j=0; j<matrixSize; j++){
                if(visitedNode[j]==0 && matrix[start][j]<min){
                    min = matrix[start][j];
                    node = j;
                }
            }
            if(node != -1){
                shortPath[node] = min;
                visitedNode[node] = 1;
                for(int k=0; k<matrixSize; k++){
                    if(visitedNode[k] == 0 && matrix[start][node] + matrix[node][k] < matrix[start][k]){
                        matrix[start][k] = matrix[start][node] + matrix[node][k];
                        String name = null;
                        for (Map.Entry<String, Integer> entry : stringToMap.entrySet()) {
                            if (entry.getValue() == k) {
                                name = entry.getKey();
                                break;
                            }
                        }
                        path[k] = path[node] + "->" + name;
                    }
                }
            }
        }
        return path;
    }


    /**
     * 计算最短路径
     * @author wpy
     * @param word1 路径首节点
     * @param word2 路径终止点
     * @param graph 图结构
     * @return 最短路径
     */
    public static String calcShortestPath(String word1, String word2, Map<String, Map<String, Integer>> graph){
        int index = 0;
        int matrixSize = graph.size();
        int[][] matrix = new int[matrixSize][matrixSize];
        // 将字符与唯一数字关联
        // 初始化邻接矩阵
        for(int i=0; i<matrixSize; i++){
            for(int j=0; j<matrixSize; j++){
                matrix[i][j] = maxValue;
            }
        }

        Map<String, Integer> stringToMap = new HashMap<>();
        for(String key : graph.keySet()){
            stringToMap.put(key, index);
            index++;
        }
        int start = stringToMap.get(word1);
        // 修改邻接矩阵值
        for(int i=0; i<matrixSize; i++){
            // String key = stringToMap.inverse().get(i);
            String key = null;
            for (Map.Entry<String, Integer> entry : stringToMap.entrySet()) {
                if (entry.getValue() == i) {
                    key = entry.getKey();
                    break;
                }
            }
            Map<String, Integer> innerMap = graph.get(key);
            for(Map.Entry<String, Integer> entry : innerMap.entrySet()){
                String innerStr = entry.getKey();
                if(innerStr == null){
                    break;
                }
                int value = entry.getValue();
                int innerStrToInt = stringToMap.get(innerStr);
                matrix[i][innerStrToInt] = value;
            }
        }
        int[] shortPath = new int[matrixSize];
        int[] visitedNode = new int[matrixSize];
        String[] path = new String[matrixSize];
        shortPath[start] = 0;
        visitedNode[start] = 1;
        for(int i=0; i<matrixSize; i++){
            String name = null;
            for (Map.Entry<String, Integer> entry : stringToMap.entrySet()) {
                if (entry.getValue() == i) {
                    name = entry.getKey();
                    break;
                }
            }
            path[i] = new String(word1 + "->" + name);
        }
        for(int i=0; i<matrixSize; i++){
            int min = maxValue;
            int node = -1;
            for(int j=0; j<matrixSize; j++){
                if(visitedNode[j]==0 && matrix[start][j]<min){
                    min = matrix[start][j];
                    node = j;
                }
            }
            if(node != -1){
                shortPath[node] = min;
                visitedNode[node] = 1;
                for(int k=0; k<matrixSize; k++){
                    if(visitedNode[k] == 0 && matrix[start][node] + matrix[node][k] < matrix[start][k]){
                        matrix[start][k] = matrix[start][node] + matrix[node][k];
                        String name = null;
                        for (Map.Entry<String, Integer> entry : stringToMap.entrySet()) {
                            if (entry.getValue() == k) {
                                name = entry.getKey();
                                break;
                            }
                        }
                        path[k] = path[node] + "->" + name;
                    }
                }
            }
        }
        int num = stringToMap.get(word2);
        return path[num];
    }


    /**
     * 随机游走函数
     * @author zpx,wpy
     * @param graph 图结构
     * @param stopRequested 中止指令
     * @return 当前完成的路径
     */
    public static String randomWalk(Map<String, Map<String, Integer>> graph, AtomicBoolean stopRequested){
        StringBuilder path = new StringBuilder();
        Set<String> visitedNode = new HashSet<>();
        Random ramdom = new Random();
        List<String> nodes = new ArrayList<>(graph.keySet());
        String currentNode = nodes.get(ramdom.nextInt(nodes.size()));
        while (!stopRequested.get() && !Thread.currentThread().isInterrupted()){
            path.append(currentNode).append(" ");
            visitedNode.add(currentNode);
            Map<String, Integer> neighbors = graph.get(currentNode);

            if(neighbors != null && !neighbors.isEmpty()){
                List<String> neighborNodes = new ArrayList<>(neighbors.keySet());
                String nextNode = neighborNodes.get(ramdom.nextInt(neighborNodes.size()));

                // 重复边
                if(visitedNode.contains(nextNode)){
                    path.append(nextNode).append("(repeat)");
                    break;
                }
                currentNode = nextNode;

                try {
                    Thread.sleep(1500);
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
            else {
                // 没有出边
                path.append("(dead node)");
                break;
            }
        }
        try (FileWriter writer = new FileWriter("path.txt",true)) {
            writer.write(path.toString());
            writer.write(System.lineSeparator()); // 添加换行符
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path.toString();
    }
}
