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
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AIML Preprocessor and substitutions
 */
public class PreProcessor {
	private static final Logger log = LoggerFactory
			.getLogger(PreProcessor.class);
    public int normalCount = 0;
    public int denormalCount = 0;
    public int personCount = 0;
    public int person2Count = 0;
    public int genderCount = 0;
    public String[] normalSubs = new String[MagicNumbers.max_substitutions];
    public Pattern[] normalPatterns = new Pattern[MagicNumbers.max_substitutions];
    public String[] denormalSubs = new String[MagicNumbers.max_substitutions];
    public Pattern[] denormalPatterns = new Pattern[MagicNumbers.max_substitutions];
    public String[] personSubs = new String[MagicNumbers.max_substitutions];
    public Pattern[] personPatterns = new Pattern[MagicNumbers.max_substitutions];
    public String[] person2Subs = new String[MagicNumbers.max_substitutions];
    public Pattern[] person2Patterns = new Pattern[MagicNumbers.max_substitutions];
    public String[] genderSubs = new String[MagicNumbers.max_substitutions];
    public Pattern[] genderPatterns = new Pattern[MagicNumbers.max_substitutions];

    /**
     * Constructor given bot
     *
     * @param bot       AIML bot
     */
    public PreProcessor (Bot bot) {
        normalCount = readSubstitutions(MagicStrings.config_path+"/normal.txt", normalPatterns, normalSubs);
        denormalCount = readSubstitutions(MagicStrings.config_path+"/denormal.txt", denormalPatterns, denormalSubs);
        personCount = readSubstitutions(MagicStrings.config_path +"/person.txt", personPatterns, personSubs);
        person2Count = readSubstitutions(MagicStrings.config_path +"/person2.txt", person2Patterns, person2Subs);
        genderCount = readSubstitutions(MagicStrings.config_path +"/gender.txt", genderPatterns, genderSubs);
        log.info("Preprocessor: "+normalCount+" norms "+personCount+" persons "+person2Count+" person2 ");
    }

    /**
     * apply normalization substitutions to a request
     *
     * @param request  client input
     * @return         normalized client input
     */
    public String normalize (String request) {
        return substitute(request,  normalPatterns, normalSubs, normalCount);
    }
    /**
     * apply denormalization substitutions to a request
     *
     * @param request  client input
     * @return         normalized client input
     */
    public String denormalize (String request) {
        return substitute(request,  denormalPatterns, denormalSubs, denormalCount);
    }
    /**
     * personal pronoun substitution for {@code <person></person>} tag
     * @param input  sentence
     * @return       sentence with pronouns swapped
     */
    public String person (String input) {
        return substitute(input, personPatterns, personSubs, personCount);

    }
    /**
     * personal pronoun substitution for {@code <person2></person2>} tag
     * @param input  sentence
     * @return       sentence with pronouns swapped
     */
    public String person2 (String input) {
        return substitute(input, person2Patterns, person2Subs, person2Count);

    }
    /**
     * personal pronoun substitution for {@code <gender>} tag
     * @param input  sentence
     * @return       sentence with pronouns swapped
     */
    public String gender (String input) {
        return substitute(input, genderPatterns, genderSubs, genderCount);

    }

    /**
     * Apply a sequence of subsitutions to an input string
     *
     * @param request      input request
     * @param patterns     array of patterns to match
     * @param subs         array of substitution values
     * @param count        number of patterns and substitutions
     * @return             result of applying substitutions to input
     */
    String substitute(String request, Pattern[] patterns, String[] subs, int count) {
        String result = " "+request+" ";
        try {
        for (int i = 0; i < count; i++) {

            String replacement =  subs[i];
            Pattern p = patterns[i];
            Matcher m = p.matcher(result);
            //log.info(i+" "+patterns[i].pattern()+"-->"+subs[i]);
            if (m.find()) {
                //log.info(m.group());
                result = m.replaceAll(replacement);
            }

            //log.info(result);
        }
        while (result.contains("  ")) result = result.replace("  "," ");
        result = result.trim();
        //log.info("Normalized: "+result);
        } catch (Exception ex)    {
            ex.printStackTrace();
        }
        return result.trim();
    }

    /**
     * read substitutions from input stream
     *
     * @param in        input stream
     * @param patterns  array of patterns
     * @param subs      array of substitution values
     * @return          number of patterns substitutions read
     */
    public int readSubstitutionsFromInputStream(InputStream in, Pattern[] patterns, String[] subs) {
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    String strLine;
    //Read File Line By Line
    int subCount = 0;
    try {
    while ((strLine = br.readLine()) != null)   {
        //log.info(strLine);
        strLine = strLine.trim();
        Pattern pattern = Pattern.compile("\"(.*?)\",\"(.*?)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(strLine);
        if (matcher.find() && subCount < MagicNumbers.max_substitutions) {
            subs[subCount] = matcher.group(2);
            String quotedPattern = Pattern.quote(matcher.group(1));
            //log.info("quoted pattern="+quotedPattern);
            patterns[subCount] = Pattern.compile(quotedPattern, Pattern.CASE_INSENSITIVE);
            subCount++;
        }

    }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    return subCount;
}

    /**
     * read substitutions from a file
     *
     * @param filename  name of substitution file
     * @param patterns  array of patterns
     * @param subs      array of substitution values
     * @return          number of patterns and substitutions read
     */
    int readSubstitutions(String filename, Pattern[] patterns, String[] subs) {
        int subCount = 0;
        try{

            // Open the file that is the first
            // command line parameter
            File file = new File(filename);
            if (file.exists()) {
                FileInputStream fstream = new FileInputStream(filename);
                // Get the object of DataInputStream
                subCount = readSubstitutionsFromInputStream(fstream, patterns, subs);
                //Close the input stream
                fstream.close();
            }
        }catch (Exception e){//Catch exception if any
            log.error("Cannot read substitutions from '" + filename + "': " + e, e);
        }
        return (subCount);
    }

    /**
     * Split an input into an array of sentences based on sentence-splitting characters.
     *
     * @param line   input text
     * @return       array of sentences
     */
    public String[] sentenceSplit(String line) {
        line = line.replace("。",".");
        line = line.replace("？","?");
        line = line.replace("！","!");
        //log.info("Sentence split "+line);
        String result[] = line.split("[\\.!\\?]");
        for (int i = 0; i < result.length; i++) result[i] = result[i].trim();
        return result;
    }

    /**
     * normalize a file consisting of sentences, one sentence per line.
     *
     * @param infile           input file
     * @param outfile          output file to write results
     */
    public void normalizeFile (String infile, String outfile) {
        try{
            BufferedWriter bw = null;
            FileInputStream fstream = new FileInputStream(infile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            bw = new BufferedWriter(new FileWriter(outfile)) ;
            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                strLine = normalize(strLine); //.toUpperCase();
                bw.write(strLine);
                bw.newLine();
            }
            bw.close();
            br.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
