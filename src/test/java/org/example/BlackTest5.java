package org.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.example.TextToGraph.*;
import static org.junit.Assert.assertEquals;

class BlackTest5 {

    @Test
    void queryBridgeWordsTest() {
        String baseFile = "aaa.txt";
        String[] words = FileProcess(baseFile);
        Map<String, Map<String, Integer>> graph = createGraph(words);
        String[] expected = {};
        String[] actual = queryBridgeWords("", "four", graph);
        Assertions.assertArrayEquals(expected, actual);
    }
}