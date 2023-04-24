package com.example.starter;

import io.vertx.core.*;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.*;
import io.vertx.ext.web.handler.BodyHandler;
import java.io.*;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.swing.SpinnerDateModel;

public class MainVerticle extends AbstractVerticle {

  private Router router;
  private HttpServer MainVerticle;
  private static FileSystem fileSystem;

  public static int readingValue(String word) {
    int sum = 0;
    try {
     // System.out.println("before converting" + word);
      int[] value = convertToNumArray(word);
    //  System.out.println("after converting " + Arrays.toString(value));
      for (int i = 0; i < value.length; i++) {
        sum = sum + value[i];
      }
    } catch (NumberFormatException e) {
   //   System.out.println("Invalid number:" + word);
      sum = readingValue(lastOne(word));
    }
   // System.out.println("Value of " + word + " is " + sum);

    return sum;
  }

  public static int[] convertToNumArray(String str) {
    String[] parts = str.split(" ");
    int[] nums = new int[parts.length];
    for (int i = 0; i < parts.length; i++) {
      nums[i] = Integer.parseInt(parts[i]);
    }
   // System.out.println("converting " + Arrays.toString(nums));
    return nums;
  }

  public static String closestValue(String word, List<String> words) {
    int targetValue = readingValue(word);
   // System.out.println("Target value is " + targetValue);
    int closestDifference = Integer.MAX_VALUE;
    String closestWord = "";
    for (String w : words) {
      if (w.replaceAll("\\s+", "").equalsIgnoreCase(word.replaceAll("\\s+", ""))) {
        if (w.equals(word)) {
          continue; // Skip the target word if it's already in the list
        }
        int currentValue = readingValue(w);
       // System.out.println("The Value of " + w + " is " + currentValue);
        int difference = Math.abs(targetValue - currentValue);
        if (difference < closestDifference) {
          closestDifference = difference;
          closestWord = w;
        }
      }
    }
    return closestWord;
  }

  // readingwordsfromfile
  private static void readWordsFromFile() {
    fileSystem.readFile(
        "wordsFile",
        result -> {
          if (result.succeeded()) {
            String content = result.result().toString();
            String[] words = content.split("\\r?\\n");
           // System.out.println(Arrays.toString(words));
            // for (String word : words) {//comment
            try (
                BufferedReader br = new BufferedReader(new FileReader("wordsFile"))) {
              String line;
              while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                  wordList.add(line);
                }
              }
            } catch (IOException e) {
              e.printStackTrace();
            }
            // }
          } else {
            System.out.println("Failed to read words from file");
          }
        });
  }

  // saving words to file
  public static void newWordsFile(String newLexical) { // Method to save the words in a file
    File file;
    FileWriter write;
    PrintWriter line;
    
    String lexical = newLexical;

    file = new File("wordsFile");
    if (!file.exists()) {
      try {
        file.createNewFile();
        write = new FileWriter(file, true);
        line = new PrintWriter(write);
        line.println((lexical));

        line.close();
        write.close();
      } catch (IOException ex) {
        System.out.println("Error de archivo");
      }
    } else {
      try {
        write = new FileWriter(file, true);
        line = new PrintWriter(write);
        line.println((lexical));
        line.close();
        write.close();
      } catch (IOException ex) {
        System.out.println("Error de archivo");
      }
    }
  }

  // Value
  public static String lastOne(String thisWord) { // Method to change words to value
    String input = thisWord;
    String value = "";
    for (int i = 0; i < input.length(); ++i) {
      char ch = input.charAt(i);
      if (!value.isEmpty()) {
        value += " ";
      }
      int n = (int) ch - (int) 'a' + 1;
      value += String.valueOf(n);
    }
   //System.out.println("last one value: " + value);
    return value;
  }

  // atr
  int value = 0;
  String lexical = "null";
  String closestValue = "null";
  private static List<String> wordList = new ArrayList<>();

  // hastext
  public static boolean hasText(String[] text) { // To see if the PUT has the word "text"
    boolean good = false;
    if (text[0].contains("text")) {
      good = true;
    }
    return good;
  }

  // extra from value
  public static String removeLastCharacter(String str) { // Value got extra char, removing them here
    String result = null;
    if ((str != null) && (str.length() > 0)) {
      result = str.substring(0, str.length() - 8);
    }
    return result;
  }

  // newmethod
  private String closestWordLexical(String text) {
    int closestDistance = Integer.MAX_VALUE;
    String closestWord = null;
    for (String word : wordList) {
      int distance = text.compareTo(word);
      if (distance < closestDistance) {
        closestDistance = distance;
        closestWord = word;
      }
    }
    return closestWord;
  }

  // main
  @Override
  public void start(Promise<Void> start) throws Exception {
    router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    fileSystem = vertx.fileSystem();
    readWordsFromFile();
    router
        .route(HttpMethod.POST, "/analyze")
        .handler(rc -> {
          JsonObject json = rc.body().asJsonObject();
          JsonObject jsonResponse = new JsonObject();
          System.out.println(json.encodePrettily());

          String[] divide = rc.body().asString().split(":");
          String[] secondD = divide[1].split("}");

          lexical = secondD[0];
          String result = lexical.replaceAll("^\"|\"$", "");

          String lastOne = removeLastCharacter(lastOne(result));

        //  System.out.println(lastOne); // value
       //   System.out.print(result); // word

          if (hasText(divide)) {
            if (!wordList.isEmpty()) {
              newWordsFile(result);
              jsonResponse.put("value", closestValue(lastOne, wordList));
              jsonResponse.put("lexical", closestWordLexical(result));
              HttpServerResponse response = rc.response();
              response.putHeader("content-type", "application/json");
              response.setChunked(true);
              // Write to the response and end it
              response.write(jsonResponse.encode());
              response.end(); // ("{\"Closest Value\": Closest lexical}");
            } else {
              wordList.add(result);
              HttpServerResponse response = rc.response();
              response.putHeader("content-type", "application/json");
              // Write to the respon se and end it
              response.end("{\"Null\":Null}");
            }
          }
        });
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(config().getInteger("http.port", 8080))
        .onSuccess(server -> {
          this.MainVerticle = server;
          start.complete();
        })
        .onFailure(start::fail);
  }
}

