package org.alicebot.ab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Helper class to write some custom AIML
 */
public class AIMLWriter {
	private static final Logger log = LoggerFactory.getLogger(AIMLWriter.class);
             public static String[][] relatives = {
                     // aunt uncle nephew niece grandma grandpa
                     {"aunt", "her", "who", "aunt"},
                     {"ant", "her", "who", "aunt"},
                     {"uncle", "his", "who", "uncle"},
                     {"friend", "his", "who", "friend"},
                     {"bestfriend", "his", "who", "bestfriend"},
                     {"niece", "her", "who", "niece"},
                     {"nephew", "his", "who", "nephew"},
                     {"grandmother", "her", "who", "grandmother"},
                     {"grandma", "her", "who", "grandmother"},
                     {"grandmom", "her", "who", "grandmother"},
                     {"mother", "her", "who", "mother"},
                     {"ma", "her", "who", "mother"},
                     {"mom", "her", "who", "mother"},
                     {"momma", "her", "who", "mother"},
                     {"mum", "her", "who", "mother"},
                     {"mumma", "her", "who", "mother"},
                     {"mommy", "her", "who", "mother"},
                     {"mummy", "her", "who", "mother"},
                     {"grandfather", "his", "who", "grandfather"},
                     {"granddad", "his", "who", "grandfather"},
                     {"father", "his", "who", "father"},
                     {"dad", "his", "who", "father"},
                     {"dada", "his", "who", "father"},
                     {"daddy", "his", "who", "father"},
                     {"husband", "his", "who", "husband"},
                     {"hubby", "his", "who", "husband"},
                     {"wife", "her", "who", "wife"},
                     {"wifey", "her", "who", "wife"},
                     {"son", "his", "who", "son"},
                     {"daughter", "her", "who", "daughter"},
                     {"brother", "his", "who", "brother"},
                     {"sister", "her", "who", "sister"},
                     {"bro", "his", "who", "brother"},
                     {"sis", "her", "who", "sister"},
                     {"boyfriend", "his", "who", "boyfriend"},
                     {"girlfriend", "her", "who", "girlfriend"}};
    public static void familiarContactAIML () {
        for (int i = 0; i < relatives.length; i++) {
            String familiar = relatives[i][0];
            String pronoun = relatives[i][1];
            String predicate = relatives[i][3];
  String aiml =
          "<category><pattern>ISFAMILIARNAME "+familiar.toUpperCase()+"</pattern>" +
                  "<template>true</template></category>\n"+
                  "<category><pattern>FAMILIARPREDICATE "+familiar.toUpperCase()+"</pattern>" +
                  "<template>"+predicate +"</template></category>\n"+
                  "<category><pattern>FAMILIARPRONOUN "+familiar.toUpperCase()+"</pattern>" +
                  "<template>"+pronoun+"</template></category>\n";



            log.info(aiml);
        }
    }
}
