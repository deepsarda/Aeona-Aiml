package org.alicebot.ab;
/* Program AB Reference AIML 2.0 implementation
        Copyright (C) 2013 ALICE A.I. Foundation
        Contact: info@alicebot.org

        This library is free software; you can redistribute it and/or
        modify it under the terms of the GNU Library General Public
        License as published by the Free Software Foundation; either
        version 2 of the License, or (at your option) any later version.

        This library is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
        Library General Public License for more details.

        You should have received a copy of the GNU Library General Public
        License along with this library; if not, write to the
        Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
        Boston, MA  02110-1301, USA.
*/

import java.util.ArrayList;
import java.util.Collections;

import org.alicebot.ab.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB {
	private static final Logger log = LoggerFactory.getLogger(AB.class);
    /**
     * Experimental class that analyzes log data and suggests
     * new AIML patterns.
     *
     */
    public static boolean shuffle_mode = true;
    public static boolean sort_mode = false;
    public static boolean filter_atomic_mode = false;
    public static boolean filter_wild_mode = false;
    public static String logfile = MagicStrings.root_path+"/data/"+MagicStrings.ab_sample_file; //normal.txt";
    public static AIMLSet passed = new AIMLSet("passed");
    public static AIMLSet testSet = new AIMLSet("1000");
    public static int runCompletedCnt;
    /**
     * Calculates the botmaster's productivity rate in
     * categories/sec when using Pattern Suggestor to create content.
     *
     * @param  runCompletedCnt  number of categories completed in this run
     * @param  timer tells elapsed time in ms
     * @see    AB
     */
    public static void productivity (int runCompletedCnt, Timer timer) {
        float time = timer.elapsedTimeMins();
        log.info("Completed "+runCompletedCnt+" in "+time+" min. Productivity "+runCompletedCnt/time+" cat/min");
    }

       /** saves a new AIML category and increments runCompletedCnt
        *
        * @param bot   the bot the category belongs to
        * @param pattern    the category's pattern (that and topic = *)
        * @param template   the category's template
        * @param filename   the filename for the category.
        */
    public static void saveCategory(Bot bot, String pattern, String template, String filename) {
        String that = "*";
        String topic = "*";
        Category c = new Category(0, pattern,  that, topic, template, filename);
        if (c.validate()) {
            bot.brain.addCategory(c);
           // bot.categories.add(c);
            bot.writeAIMLIFFiles();
            runCompletedCnt++;
        }
        else log.info("Invalid Category "+c.validationMessage);
    }

       /** mark a category as deleted
        *
        * @param bot     the bot the category belogs to
        * @param c       the category
        */
    public static void deleteCategory(Bot bot, Category c) {
        c.setFilename(MagicStrings.deleted_aiml_file);
        c.setTemplate(MagicStrings.deleted_template);
        bot.deletedGraph.addCategory(c);
        bot.writeDeletedIFCategories();
    }

       /** skip a category.  Make the category as "unfinished"
        *
        * @param bot  the bot the category belogs to
        * @param c   the category
        */
    public static void skipCategory(Bot bot, Category c) {
        c.setFilename(MagicStrings.unfinished_aiml_file);
        c.setTemplate(MagicStrings.unfinished_template);
        bot.unfinishedGraph.addCategory(c);
        log.info(bot.unfinishedGraph.getCategories().size() + " unfinished categories");
        bot.writeUnfinishedIFCategories();
    }
    public static void abwq(Bot bot) {
        Timer timer = new Timer();
        timer.start();
        bot.classifyInputs(logfile);
        log.info(timer.elapsedTimeSecs() + " classifying inputs");
        bot.writeQuit();
    }
       /**  magically suggests new patterns for a bot.
        * Reads an input file of sample data called logFile.
        * Builds a graph of all the inputs.
        * Finds new patterns in the graph that are not already in the bot.
        * Classifies input log into those new patterns.
        *
        *
        * @param bot        the bot to perform the magic on
        */
    public static void ab (Bot bot) {
        String logFile = logfile;
        MagicBooleans.trace_mode = false;
        MagicBooleans.enable_external_sets = false;
        Timer timer = new Timer();
        bot.brain.nodeStats();
        timer.start();
        log.info("Graphing inputs");
        bot.graphInputs(logFile);
        log.info(timer.elapsedTimeSecs() + " seconds Graphing inputs");
        //bot.inputGraph.printgraph();
        timer.start();
        log.info("Finding Patterns");
        bot.findPatterns();
        log.info(bot.suggestedCategories.size()+" suggested categories");
        log.info(timer.elapsedTimeSecs() + " seconds finding patterns");
        timer.start();
        bot.patternGraph.nodeStats();
        log.info("Classifying Inputs");
        bot.classifyInputs(logFile);
        log.info(timer.elapsedTimeSecs() + " classifying inputs");
    }

       /** train the bot through a terminal interaction
        *
        * @param bot         the bot to train
        */
    public static void terminalInteraction (Bot bot) {
        Timer timer = new Timer();
        sort_mode = !shuffle_mode;
       // if (sort_mode)
        Collections.sort(bot.suggestedCategories, Category.ACTIVATION_COMPARATOR);
        ArrayList<Category> topSuggestCategories = new ArrayList<Category>();
        for (int i = 0; i < 10000 && i < bot.suggestedCategories.size(); i++) {
            topSuggestCategories.add(bot.suggestedCategories.get(i));
        }
        bot.suggestedCategories = topSuggestCategories;
        if (shuffle_mode) Collections.shuffle(bot.suggestedCategories);
        timer = new Timer();
        timer.start();
        runCompletedCnt = 0;
        ArrayList<Category> filteredAtomicCategories = new ArrayList<Category>();
        ArrayList<Category> filteredWildCategories = new ArrayList<Category>();
        for (Category c : bot.suggestedCategories) if (!c.getPattern().contains("*")) filteredAtomicCategories.add(c);
        else filteredWildCategories.add(c);
        ArrayList <Category> browserCategories;
        if (filter_atomic_mode) browserCategories = filteredAtomicCategories;
        else if (filter_wild_mode) browserCategories = filteredWildCategories;
        else browserCategories = bot.suggestedCategories;
        // log.info(filteredAtomicCategories.size()+" filtered suggested categories");
        for (Category c : browserCategories)  {
            try {
            ArrayList samples = new ArrayList(c.getMatches());
            Collections.shuffle(samples);
            int sampleSize = Math.min(MagicNumbers.displayed_input_sample_size, c.getMatches().size());
            for (int i = 0; i < sampleSize; i++) {
                log.info("" + samples.get(i));
            }
            log.info("["+c.getActivationCnt()+"] "+c.inputThatTopic());
            productivity(runCompletedCnt, timer);
            String textLine = "" + IOUtils.readInputTextLine();
            terminalInteractionStep(bot, "", textLine, c);
            } catch (Exception ex) {
                ex.printStackTrace();
                log.info("Returning to Category Browser");
            }
        }
    }

       /** process one step of the terminal interaction
        *
        * @param bot     the bot being trained.
        * @param request      used when this routine is called by benchmark testSuite
        * @param textLine     response typed by the botmaster
        * @param c            AIML category selected
        */
   public static void terminalInteractionStep (Bot bot, String request, String textLine, Category c) {
       String template = null;
       if (textLine.contains("<pattern>") && textLine.contains("</pattern>")) {
           int index = textLine.indexOf("<pattern>")+"<pattern>".length();
           int jndex = textLine.indexOf("</pattern>");
           int kndex = jndex + "</pattern>".length();
           if (index < jndex) {
               String pattern = textLine.substring(index, jndex);
               c.setPattern(pattern);
               textLine = textLine.substring(kndex, textLine.length());
               log.info("Got pattern = "+pattern+" template = "+textLine);
           }
       }
       String botThinks = "";
       String[] pronouns = {"he", "she", "it", "we", "they"};
       for (String p : pronouns) {
           if (textLine.contains("<"+p+">")) {
               textLine = textLine.replace("<"+p+">","");
               botThinks = "<think><set name=\""+p+"\"><set name=\"topic\"><star/></set></set></think>";
           }
       }
       if (textLine.equals("q")) System.exit(0);       // Quit program
       else if (textLine.equals("wq")) {   // Write AIML Files and quit program
           bot.writeQuit();
         /*  Nodemapper udcNode = bot.brain.findNode("*", "*", "*");
           if (udcNode != null) {
               AIMLSet udcMatches = new AIMLSet("udcmatches");
               udcMatches.addAll(udcNode.category.getMatches());
               udcMatches.writeAIMLSet();
           }*/
          /* Nodemapper cNode = bot.brain.match("JOE MAKES BEER", "unknown", "unknown");
           if (cNode != null) {
               AIMLSet cMatches = new AIMLSet("cmatches");
               cMatches.addAll(cNode.category.getMatches());
               cMatches.writeAIMLSet();
           }
           if (passed.size() > 0) {
               AIMLSet difference = new AIMLSet("difference");
               AIMLSet intersection = new AIMLSet("intersection");
               for (String s : passed) if (testSet.contains(s)) intersection.add(s);
               passed = intersection;
               passed.setName = "passed";
               difference.addAll(testSet);
               difference.removeAll(passed);
               difference.writeAIMLSet();

               passed.writeAIMLSet();
               testSet.writeAIMLSet();
               log.info("Wrote passed test cases");
           }*/
           System.exit(0);
       }
       else if (textLine.equals("skip") || textLine.equals("")) { // skip this one for now
           skipCategory(bot, c);
       }
       else if (textLine.equals("s") || textLine.equals("pass")) { // skip this one for now
           passed.add(request);
           AIMLSet difference = new AIMLSet("difference");
           difference.addAll(testSet);
           difference.removeAll(passed);
           difference.writeAIMLSet();
           passed.writeAIMLSet();
       }
       else if (textLine.equals("d")) { // delete this suggested category
           deleteCategory(bot, c);
       }
       else if (textLine.equals("x")) {    // ask another bot
           template = "<sraix>"+c.getPattern().replace("*","<star/>")+"</sraix>";
           template += botThinks;
           saveCategory(bot, c.getPattern(), template, MagicStrings.sraix_aiml_file);
       }
       else if (textLine.equals("p")) {   // filter inappropriate content
           template = "<srai>"+MagicStrings.inappropriate_filter+"</srai>";
           template += botThinks;
           saveCategory(bot, c.getPattern(), template, MagicStrings.inappropriate_aiml_file);
       }
       else if (textLine.equals("f")) { // filter profanity
           template = "<srai>"+MagicStrings.profanity_filter+"</srai>";
           template += botThinks;
           saveCategory(bot, c.getPattern(), template, MagicStrings.profanity_aiml_file);
       }
       else if (textLine.equals("i")) {
           template = "<srai>"+MagicStrings.insult_filter+"</srai>";
           template += botThinks;
           saveCategory(bot, c.getPattern(), template, MagicStrings.insult_aiml_file);
       }
       else if (textLine.contains("<srai>") ||  textLine.contains("<sr/>"))  {
           template = textLine;
           template += botThinks;
           saveCategory(bot, c.getPattern(), template, MagicStrings.reductions_update_aiml_file);
       }
       else if (textLine.contains("<oob>"))  {
           template = textLine;
           template += botThinks;
           saveCategory(bot, c.getPattern(), template, MagicStrings.oob_aiml_file);
       }
       else if (textLine.contains("<set name") || botThinks.length() > 0) {
           template = textLine;
           template += botThinks;
           saveCategory(bot, c.getPattern(), template, MagicStrings.predicates_aiml_file);
       }
       else if (textLine.contains("<get name") && !textLine.contains("<get name=\"name")) {
           template = textLine;
           template += botThinks;
           saveCategory(bot, c.getPattern(), template, MagicStrings.predicates_aiml_file);
       }
       else {
           template = textLine;
           template += botThinks;
           saveCategory(bot, c.getPattern(), template, MagicStrings.personality_aiml_file);
       }

   }

}

