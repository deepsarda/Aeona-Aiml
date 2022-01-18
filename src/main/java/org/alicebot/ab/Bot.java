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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representing the AIML bot
 */
public class Bot {
	private static final Logger log = LoggerFactory.getLogger(Bot.class);
    public final Properties properties = new Properties();
    public final PreProcessor preProcessor;
    public final Graphmaster brain;
    public final Graphmaster inputGraph;
    public final Graphmaster learnfGraph;
    public final Graphmaster patternGraph;
    public final Graphmaster deletedGraph;
    public Graphmaster unfinishedGraph;
  //  public final ArrayList<Category> categories;
    public ArrayList<Category> suggestedCategories;
    public String name=MagicStrings.unknown_bot_name;
    public HashMap<String, AIMLSet> setMap = new HashMap<String, AIMLSet>();
    public HashMap<String, AIMLMap> mapMap = new HashMap<String, AIMLMap>();

    /**
     * Set all directory path variables for this bot
     *
     * @param root        root directory of Program AB
     * @param name        name of bot
     */
    public void setAllPaths (String root, String name) {
        MagicStrings.bot_path = root+"/bots";
        MagicStrings.bot_name_path = MagicStrings.bot_path+"/"+name;
        log.info("Name = {} Path = {}", name, MagicStrings.bot_name_path);
        MagicStrings.aiml_path = MagicStrings.bot_name_path+"/aiml";
        MagicStrings.aimlif_path = MagicStrings.bot_name_path+"/aimlif";
        MagicStrings.config_path = MagicStrings.bot_name_path+"/config";
        MagicStrings.log_path = MagicStrings.bot_name_path+"/logs";
        MagicStrings.sets_path = MagicStrings.bot_name_path+"/sets";
        MagicStrings.maps_path = MagicStrings.bot_name_path+"/maps";
        log.info(MagicStrings.root_path);
        log.info(MagicStrings.bot_path);
        log.info(MagicStrings.bot_name_path);
        log.info(MagicStrings.aiml_path);
        log.info(MagicStrings.aimlif_path);
        log.info(MagicStrings.config_path);
        log.info(MagicStrings.log_path);
        log.info(MagicStrings.sets_path);
        log.info(MagicStrings.maps_path);
    }

    /**
     * Constructor (default action, default path, default bot name)
     */
    public Bot() {
        this(MagicStrings.default_bot);
    }

    /**
     * Constructor (default action, default path)
     * @param name
     */
    public Bot(String name) {
        this(name, MagicStrings.root_path);
    }

    /**
     * Constructor (default action)
     *
     * @param name
     * @param path
     */
    public Bot(String name, String path) {
        this(name, path, "auto");
    }

    /**
     * Constructor
     *
     * @param name     name of bot
     * @param path     root path of Program AB
     * @param action   Program AB action
     */
    public Bot(String name, String path, String action) {
        this.name = name;
        setAllPaths(path, name);
        this.brain = new Graphmaster(this);
        this.inputGraph = new Graphmaster(this);
        this.learnfGraph = new Graphmaster(this);
        this.deletedGraph = new Graphmaster(this);
        this.patternGraph = new Graphmaster(this);
        this.unfinishedGraph = new Graphmaster(this);
      //  this.categories = new ArrayList<Category>();
        this.suggestedCategories = new ArrayList<Category>();
        preProcessor = new PreProcessor(this);
        addProperties();
        addAIMLSets();
        addAIMLMaps();
        AIMLSet number = new AIMLSet(MagicStrings.natural_number_set_name);
        setMap.put(MagicStrings.natural_number_set_name, number);
        AIMLMap successor = new AIMLMap(MagicStrings.map_successor);
        mapMap.put(MagicStrings.map_successor, successor);
        AIMLMap predecessor = new AIMLMap(MagicStrings.map_predecessor);
        mapMap.put(MagicStrings.map_predecessor, predecessor);
        //log.info("setMap = "+setMap);
        Date aimlDate = new Date(new File(MagicStrings.aiml_path).lastModified());
        Date aimlIFDate = new Date(new File(MagicStrings.aimlif_path).lastModified());
        log.info("AIML modified {} AIMLIF modified {}", aimlDate, aimlIFDate);
        readDeletedIFCategories();
        readUnfinishedIFCategories();
        MagicStrings.pannous_api_key = Utilities.getPannousAPIKey();
        MagicStrings.pannous_login = Utilities.getPannousLogin();
        if (action.equals("aiml2csv")) addCategoriesFromAIML();
        else if (action.equals("csv2aiml")) addCategoriesFromAIMLIF();
        else if (aimlDate.after(aimlIFDate)) {
            log.info("AIML modified after AIMLIF");
            addCategoriesFromAIML();
            writeAIMLIFFiles();
        }
        else {
            addCategoriesFromAIMLIF();
            if (brain.getCategories().size()==0) {
                log.info("No AIMLIF Files found.  Looking for AIML");
                addCategoriesFromAIML();
            }
        }
        log.info("--> Bot {} {} completed {} deleted {} unfinished",
        		name, brain.getCategories().size(), deletedGraph.getCategories().size(), unfinishedGraph.getCategories().size());
    }

    /**
     * add an array list of categories with a specific file name
     *
     * @param file      name of AIML file
     * @param moreCategories    list of categories
     */
    void addMoreCategories (String file, ArrayList<Category> moreCategories) {
        if (file.contains(MagicStrings.deleted_aiml_file)) {
            for (Category c : moreCategories) {
                //log.info("Delete "+c.getPattern());
                deletedGraph.addCategory(c);
            }
        } else if (file.contains(MagicStrings.unfinished_aiml_file)) {
            for (Category c : moreCategories) {
                //log.info("Delete "+c.getPattern());
                if (brain.findNode(c) == null)
                unfinishedGraph.addCategory(c);
                else log.info("unfinished {} found in brain", c.inputThatTopic());
            }
        } else if (file.contains(MagicStrings.learnf_aiml_file) ) {
            log.info("Reading Learnf file");
            for (Category c : moreCategories) {
                brain.addCategory(c);
                learnfGraph.addCategory(c);
                patternGraph.addCategory(c);
            }
            //this.categories.addAll(moreCategories);
        } else {
            for (Category c : moreCategories) {
                //log.info("Brain size="+brain.root.size());
                //brain.printgraph();
                brain.addCategory(c);
                patternGraph.addCategory(c);
                //brain.printgraph();
            }
            //this.categories.addAll(moreCategories);
        }
    }

    /**
     * Load all brain categories from AIML directory
     */
    void addCategoriesFromAIML() {
        Timer timer = new Timer();
        timer.start();
        try {
            // Directory path here
            String file;
            File folder = new File(MagicStrings.aiml_path);
            if (folder.exists()) {
                File[] listOfFiles = folder.listFiles();
                log.info("Loading AIML files from '{}'", MagicStrings.aiml_path);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        file = listOfFile.getName();
                        if (file.endsWith(".aiml") || file.endsWith(".AIML")) {
                            log.info(file);
                            try {
                                ArrayList<Category> moreCategories = AIMLProcessor.AIMLToCategories(MagicStrings.aiml_path, file);
                                addMoreCategories(file, moreCategories);
                            } catch (Exception iex) {
                                log.error("Problem loading '" + file +"': " + iex, iex);
                            }
                        }
                    }
                }
            }
            else log.info("addCategories: '{}' does not exist.", MagicStrings.aiml_path);
        } catch (Exception ex)  {
            ex.printStackTrace();
        }
        log.info("Loaded {} categories in {} sec", brain.getCategories().size(), timer.elapsedTimeSecs());
    }

    /**
     * load all brain categories from AIMLIF directory
     */
    void addCategoriesFromAIMLIF() {
        Timer timer = new Timer();
        timer.start();
        try {
            // Directory path here
            String file;
            File folder = new File(MagicStrings.aimlif_path);
            if (folder.exists()) {
                File[] listOfFiles = folder.listFiles();
                log.info("Loading AIML files from '{}'", MagicStrings.aimlif_path);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        file = listOfFile.getName();
                        if (file.endsWith(MagicStrings.aimlif_file_suffix) || file.endsWith(MagicStrings.aimlif_file_suffix.toUpperCase())) {
                            //log.info(file);
                            try {
                                ArrayList<Category> moreCategories = readIFCategories(MagicStrings.aimlif_path + "/" + file);
                                addMoreCategories(file, moreCategories);
                             //   MemStats.memStats();
                            } catch (Exception iex) {
                                log.error("Problem loading '" + file + "': " + iex, iex);
                            }
                        }
                    }
                }
            }
            else log.info("addCategories: '{}' does not exist.", MagicStrings.aimlif_path);
        } catch (Exception ex)  {
            ex.printStackTrace();
        }
        log.info("Loaded {} categories in {} sec", brain.getCategories().size(), timer.elapsedTimeSecs());
    }

    /**
     * read deleted categories from AIMLIF file
     */
    public void readDeletedIFCategories() {
        readCertainIFCategories(deletedGraph, MagicStrings.deleted_aiml_file);
    }

    /**
     * read unfinished categories from AIMLIF file
     */
    public void readUnfinishedIFCategories() {
        readCertainIFCategories(unfinishedGraph, MagicStrings.unfinished_aiml_file);
    }

    /**
     * update unfinished categories removing any categories that have been finished
     */
    public void updateUnfinishedCategories () {
        ArrayList<Category> unfinished = unfinishedGraph.getCategories();
        unfinishedGraph = new Graphmaster(this);
        for (Category c : unfinished) {
            if (!brain.existsCategory(c)) unfinishedGraph.addCategory(c);
        }
    }

    /**
     * write all AIML and AIMLIF categories
     */
    public void writeQuit() {
        writeAIMLIFFiles();
        log.info("Wrote AIMLIF Files");
        writeAIMLFiles();
        log.info("Wrote AIML Files");
        writeDeletedIFCategories();
        updateUnfinishedCategories();
        writeUnfinishedIFCategories();

    }

    /**
     * read categories from specified AIMLIF file into specified graph
     *
     * @param graph   Graphmaster to store categories
     * @param fileName   file name of AIMLIF file
     */
    public void readCertainIFCategories(Graphmaster graph, String fileName) {
        File file = new File(MagicStrings.aimlif_path+"/"+fileName+MagicStrings.aimlif_file_suffix);
        if (file.exists()) {
            try {
                ArrayList<Category> deletedCategories = readIFCategories(MagicStrings.aimlif_path+"/"+fileName+MagicStrings.aimlif_file_suffix);
                for (Category d : deletedCategories) graph.addCategory(d);
                log.info("readCertainIFCategories {} categories from {}", graph.getCategories().size(), fileName+MagicStrings.aimlif_file_suffix);
            } catch (Exception iex) {
                log.error("Problem loading '" + fileName + "': " + iex, iex);
            }
        }
        else log.info("No "+MagicStrings.deleted_aiml_file+MagicStrings.aimlif_file_suffix+" file found");
    }

    /**
     * write certain specified categories as AIMLIF files
     *
     * @param graph       the Graphmaster containing the categories to write
     * @param file        the destination AIMLIF file
     */
    public void writeCertainIFCategories(Graphmaster graph, String file) {
        if (MagicBooleans.trace_mode) log.info("writeCertainIFCaegories "+file+" size= "+graph.getCategories().size());
        writeIFCategories(graph.getCategories(), file+MagicStrings.aimlif_file_suffix);
        File dir = new File(MagicStrings.aimlif_path);
        dir.setLastModified(new Date().getTime());
    }

    /**
     * write deleted categories to AIMLIF file
     */
    public void writeDeletedIFCategories() {
        writeCertainIFCategories(deletedGraph, MagicStrings.deleted_aiml_file);
    }

    /**
     * write learned categories to AIMLIF file
     */
    public void writeLearnfIFCategories() {
        writeCertainIFCategories(learnfGraph, MagicStrings.learnf_aiml_file);
    }

    /**
     * write unfinished categories to AIMLIF file
     */
    public void writeUnfinishedIFCategories() {
        writeCertainIFCategories(unfinishedGraph, MagicStrings.unfinished_aiml_file);
    }

    /**
     * write categories to AIMLIF file
     *
     * @param cats           array list of categories
     * @param filename       AIMLIF filename
     */
    public void writeIFCategories (ArrayList<Category> cats, String filename)  {
        //log.info("writeIFCategories "+filename);
        BufferedWriter bw = null;
        File existsPath = new File(MagicStrings.aimlif_path);
        if (existsPath.exists())
        try {
            //Construct the bw object
            bw = new BufferedWriter(new FileWriter(MagicStrings.aimlif_path+"/"+filename)) ;
            for (Category category : cats) {
                bw.write(Category.categoryToIF(category));
                bw.newLine();
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the bw
            try {
                if (bw != null) {
                    bw.flush();
                    bw.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Write all AIMLIF files from bot brain
     */
    public void writeAIMLIFFiles () {
        log.info("writeAIMLIFFiles");
        HashMap<String, BufferedWriter> fileMap = new HashMap<String, BufferedWriter>();
        if (deletedGraph.getCategories().size() > 0) writeDeletedIFCategories();
        ArrayList<Category> brainCategories = brain.getCategories();
        Collections.sort(brainCategories, Category.CATEGORY_NUMBER_COMPARATOR);
        for (Category c : brainCategories) {
            try {
                BufferedWriter bw;
                String fileName = c.getFilename();
                if (fileMap.containsKey(fileName)) bw = fileMap.get(fileName);
                else {
                    bw = new BufferedWriter(new FileWriter(MagicStrings.aimlif_path+"/"+fileName+MagicStrings.aimlif_file_suffix));
                    fileMap.put(fileName, bw);

                }
                bw.write(Category.categoryToIF(c));
                bw.newLine();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        Set set = fileMap.keySet();
        for (Object aSet : set) {
            BufferedWriter bw = fileMap.get(aSet);
            //Close the bw
            try {
                if (bw != null) {
                    bw.flush();
                    bw.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();

            }

        }
        File dir = new File(MagicStrings.aimlif_path);
        dir.setLastModified(new Date().getTime());
    }

    /**
     * Write all AIML files.  Adds categories for BUILD and DEVELOPMENT ENVIRONMENT
     */
    public void writeAIMLFiles () {
        HashMap<String, BufferedWriter> fileMap = new HashMap<String, BufferedWriter>();
        Category b = new Category(0, "BUILD", "*", "*", new Date().toString(), "update.aiml");
        brain.addCategory(b);
        b = new Category(0, "DELEVLOPMENT ENVIRONMENT", "*", "*", MagicStrings.programNameVersion, "update.aiml");
        brain.addCategory(b);
        ArrayList<Category> brainCategories = brain.getCategories();
        Collections.sort(brainCategories, Category.CATEGORY_NUMBER_COMPARATOR);
        for (Category c : brainCategories) {

            if (!c.getFilename().equals(MagicStrings.null_aiml_file))
            try {
                //log.info("Writing "+c.getCategoryNumber()+" "+c.inputThatTopic());
                BufferedWriter bw;
                String fileName = c.getFilename();
                if (fileMap.containsKey(fileName)) bw = fileMap.get(fileName);
                else {
                    String copyright = Utilities.getCopyright(this, fileName);
                    bw = new BufferedWriter(new FileWriter(MagicStrings.aiml_path+"/"+fileName));
                    fileMap.put(fileName, bw);
                    bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
                            "<aiml>\n");
                    bw.write(copyright);
                     //bw.newLine();
                }
                bw.write(Category.categoryToAIML(c)+"\n");
                //bw.newLine();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        Set set = fileMap.keySet();
        for (Object aSet : set) {
            BufferedWriter bw = fileMap.get(aSet);
            //Close the bw
            try {
                if (bw != null) {
                    bw.write("</aiml>\n");
                    bw.flush();
                    bw.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();

            }

        }
        File dir = new File(MagicStrings.aiml_path);
        dir.setLastModified(new Date().getTime());
    }

    /**
     * load bot properties
     */
    void addProperties() {
        try {
            properties.getProperties(MagicStrings.config_path+"/properties.txt");
        } catch (Exception ex)  {
            ex.printStackTrace();
        }
    }

    static int leafPatternCnt = 0;
    static int starPatternCnt = 0;

    /** find suggested patterns in a graph of inputs
     *
     */
    public void findPatterns() {
        findPatterns(inputGraph.root, "");
        log.info("{} Leaf Patterns {} Star Patterns", leafPatternCnt, starPatternCnt);
    }

    /** find patterns recursively
     *
     * @param node                      current graph node
     * @param partialPatternThatTopic   partial pattern path
     */
    void findPatterns(Nodemapper node, String partialPatternThatTopic) {
        if (NodemapperOperator.isLeaf(node)) {
            //log.info("LEAF: "+node.category.getActivationCnt()+". "+partialPatternThatTopic);
            if (node.category.getActivationCnt() > MagicNumbers.node_activation_cnt) {
                //log.info("LEAF: "+node.category.getActivationCnt()+". "+partialPatternThatTopic+" "+node.shortCut);    //Start writing to the output stream
                leafPatternCnt ++;
                try {
                    String categoryPatternThatTopic = "";
                    if (node.shortCut) {
                        //log.info("Partial patternThatTopic = "+partialPatternThatTopic);
                        categoryPatternThatTopic = partialPatternThatTopic + " <THAT> * <TOPIC> *";
                    }
                    else categoryPatternThatTopic = partialPatternThatTopic;
                    Category c = new Category(0, categoryPatternThatTopic,  MagicStrings.blank_template, MagicStrings.unknown_aiml_file);
                    //if (brain.existsCategory(c)) log.info(c.inputThatTopic()+" Exists");
                    //if (deleted.existsCategory(c)) log.info(c.inputThatTopic()+ " Deleted");
                    if (!brain.existsCategory(c) && !deletedGraph.existsCategory(c) && !unfinishedGraph.existsCategory(c)) {
                        patternGraph.addCategory(c);
                        suggestedCategories.add(c);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if(NodemapperOperator.size(node) > MagicNumbers.node_size) {
            //log.info("STAR: "+NodemapperOperator.size(node)+". "+partialPatternThatTopic+" * <that> * <topic> *");
            starPatternCnt ++;
            try {
                Category c = new Category(0, partialPatternThatTopic+" * <THAT> * <TOPIC> *",  MagicStrings.blank_template, MagicStrings.unknown_aiml_file);
                //if (brain.existsCategory(c)) log.info(c.inputThatTopic()+" Exists");
                //if (deleted.existsCategory(c)) log.info(c.inputThatTopic()+ " Deleted");
                if (!brain.existsCategory(c) && !deletedGraph.existsCategory(c) && !unfinishedGraph.existsCategory(c)) {
                    patternGraph.addCategory(c);
                    suggestedCategories.add(c);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (String key : NodemapperOperator.keySet(node)) {
            Nodemapper value = NodemapperOperator.get(node, key);
            findPatterns(value, partialPatternThatTopic + " " + key);
        }

    }

    /** classify inputs into matching categories
     *
     * @param filename    file containing sample normalized inputs
     */
    public void classifyInputs (String filename) {
        try{
            FileInputStream fstream = new FileInputStream(filename);
            // Get the object
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            //Read File Line By Line
            int count = 0;
            while ((strLine = br.readLine())!= null)   {
                // Print the content on the console
                //log.info("Classifying "+strLine);
                if (strLine.startsWith("Human: ")) strLine = strLine.substring("Human: ".length(), strLine.length());
                Nodemapper match = patternGraph.match(strLine, "unknown", "unknown");
                match.category.incrementActivationCnt();
                count += 1;
            }
            //Close the input stream
            br.close();
        } catch (Exception e){//Catch exception if any
            log.error("Cannot classify inputs from '" + filename + "': " + e, e);
        }
    }

    /** read sample inputs from filename, turn them into Paths, and
     * add them to the graph.
     *
     * @param filename file containing sample inputs
     */
    public void graphInputs (String filename) {
        try{
            // Open the file that is the first
            // command line parameter
            FileInputStream fstream = new FileInputStream(filename);
            // Get the object
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                //strLine = preProcessor.normalize(strLine);
                Category c = new Category(0, strLine, "*", "*", "nothing", MagicStrings.unknown_aiml_file);
                Nodemapper node = inputGraph.findNode(c);
                if (node == null) {
                  inputGraph.addCategory(c);
                  c.incrementActivationCnt();
                }
                else node.category.incrementActivationCnt();
                //log.info("Root branches="+g.root.size());
            }
            //Close the input stream
            br.close();
        }catch (Exception e){//Catch exception if any
            log.error("Cannot graph inputs from '" + filename + "': " + e, e);
        }
    }



    /**
     * read AIMLIF categories from a file into bot brain
     *
     * @param filename    name of AIMLIF file
     * @return   array list of categories read
     */
    public ArrayList<Category> readIFCategories (String filename) {
        ArrayList<Category> categories = new ArrayList<Category>();
        try{
            // Open the file that is the first
            // command line parameter
            FileInputStream fstream = new FileInputStream(filename);
            // Get the object
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                try {
                    Category c = Category.IFToCategory(strLine);
                    categories.add(c);
                } catch (Exception ex) {
                    log.warn("Invalid AIMLIF in "+filename+" line "+strLine, ex);
                }
            }
            //Close the input stream
            br.close();
        }catch (Exception e){//Catch exception if any
            log.error("Cannot read IF Categories from '" + filename + "': " + e, e);
        }
        return categories;
    }

    /**
     * check Graphmaster for shadowed categories
     */
    public void shadowChecker () {
        shadowChecker(brain.root) ;
    }

    /** traverse graph and test all categories found in leaf nodes for shadows
     *
     * @param node
     */
    void shadowChecker(Nodemapper node) {
        if (NodemapperOperator.isLeaf(node)) {
            String input = node.category.getPattern().replace("*", "XXX").replace("_", "XXX");
            String that = node.category.getThat().replace("*", "XXX").replace("_", "XXX");
            String topic = node.category.getTopic().replace("*", "XXX").replace("_", "XXX");
            Nodemapper match = brain.match(input, that, topic);
            if (match != node) {
                log.info(Graphmaster.inputThatTopic(input, that, topic));
                log.info("MATCHED:      {}", match.category.inputThatTopic());
                log.info("SHOULD MATCH: {}", node.category.inputThatTopic());
            }
        }
        else {
            for (String key : NodemapperOperator.keySet(node)) {
                shadowChecker(NodemapperOperator.get(node, key));
            }
        }
    }

    /**
     * Load all AIML Sets
     */
    void addAIMLSets() {
        Timer timer = new Timer();
        timer.start();
        try {
            // Directory path here
            String file;
            File folder = new File(MagicStrings.sets_path);
            if (folder.exists()) {
                File[] listOfFiles = folder.listFiles();
                log.info("Loading AIML Sets files from '{}'", MagicStrings.sets_path);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        file = listOfFile.getName();
                        if (file.endsWith(".txt") || file.endsWith(".TXT")) {
                            log.info(file);
                            String setName = file.substring(0, file.length()-".txt".length());
                            log.info("Read AIML Set {}", setName);
                            AIMLSet aimlSet = new AIMLSet(setName);
                            aimlSet.readAIMLSet(this);
                            setMap.put(setName, aimlSet);
                        }
                    }
                }
            }
            else log.info("addAIMLSets: {} does not exist.", MagicStrings.sets_path);
        } catch (Exception ex)  {
            ex.printStackTrace();
        }
    }

    /**
     * Load all AIML Maps
     */
    void addAIMLMaps() {
        Timer timer = new Timer();
        timer.start();
        try {
            // Directory path here
            String file;
            File folder = new File(MagicStrings.maps_path);
            if (folder.exists()) {
                File[] listOfFiles = folder.listFiles();
                log.info("Loading AIML Map files from '{}'", MagicStrings.maps_path);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        file = listOfFile.getName();
                        if (file.endsWith(".txt") || file.endsWith(".TXT")) {
                            log.info(file);
                            String mapName = file.substring(0, file.length()-".txt".length());
                            log.info("Read AIML Map "+mapName);
                            AIMLMap aimlMap = new AIMLMap(mapName);
                            aimlMap.readAIMLMap(this);
                            mapMap.put(mapName, aimlMap);
                        }
                    }
                }
            }
            else log.info("addCategories: '{}' does not exist.", MagicStrings.aiml_path);
        } catch (Exception ex)  {
            ex.printStackTrace();
        }
    }

}
