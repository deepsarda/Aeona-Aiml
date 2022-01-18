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

/**
 * Global values for many strings in Program AB
 */
public class MagicStrings {
    // General global strings
    public static String programNameVersion = "Program AB 0.0.4.3 beta -- AI Foundation Reference AIML 2.0 implementation";
    public static String comment = "removed some recursion from Path";
    public static String aimlif_split_char = ",";
    public static String default_bot = "super";
    public static String default_language = "EN";
    public static String aimlif_split_char_name = "\\#Comma";
    public static String aimlif_file_suffix = ".csv";
    public static String ab_sample_file = "sample.txt";
    // <sraix> defaults
    public static String pannous_api_key = "guest";
    public static String pannous_login = "test-user";
    public static String sraix_failed = "SRAIXFAILED";
    public static String sraix_no_hint = "nohint";
    public static String sraix_event_hint = "event";
    public static String sraix_pic_hint = "pic";
    // AIML files
    public static String unknown_aiml_file = "unknown_aiml_file.aiml";
    public static String deleted_aiml_file = "deleted.aiml";
    public static String learnf_aiml_file = "learnf.aiml";
    public static String null_aiml_file = "null.aiml";
    public static String inappropriate_aiml_file = "inappropriate.aiml";
    public static String profanity_aiml_file = "profanity.aiml";
    public static String insult_aiml_file = "insults.aiml";
    public static String reductions_update_aiml_file = "reductions_update.aiml";
    public static String predicates_aiml_file = "client_profile.aiml";
    public static String update_aiml_file = "update.aiml";
    public static String personality_aiml_file = "personality.aiml";
    public static String sraix_aiml_file = "sraix.aiml";
    public static String oob_aiml_file = "oob.aiml";
    public static String unfinished_aiml_file = "unfinished.aiml";
    // filter responses
    public static String inappropriate_filter = "FILTER INAPPROPRIATE";
    public static String profanity_filter = "FILTER PROFANITY";
    public static String insult_filter = "FILTER INSULT";
    // default templates
    public static String deleted_template = "deleted";
    public static String unfinished_template = "unfinished";
    // AIML defaults
    public static String unknown_history_item = "unknown";
    public static String default_bot_response = "I have no answer for that.";
    public static String error_bot_response = "Something is wrong with my brain.";
    public static String schedule_error = "I'm unable to schedule that event.";
    public static String system_failed = "Failed to execute system command.";
    public static String unknown_predicate_value = "unknown";
    public static String unknown_property_value = "unknown";
    public static String unknown_map_value = "unknown";
    public static String unknown_customer_id = "unknown";
    public static String unknown_bot_name = "unknown";
    public static String default_that = "unknown";
    public static String default_topic = "unknown";
    public static String template_failed = "Template failed.";
    public static String too_much_recursion = "Too much recursion in AIML";
    public static String too_much_looping = "Too much looping in AIML";
    public static String blank_template = "blank template";
    public static String null_input = "NORESP";
    public static String null_star = "nullstar";
    // sets and maps
    public static String set_member_string = "ISA";
    public static String remote_map_key = "external";
    public static String remote_set_key = "external";
    public static String natural_number_set_name = "number";
    public static String map_successor = "successor";
    public static String map_predecessor = "predecessor";
    // paths
    public static String root_path = "c:/ab";
    public static String bot_path = root_path+"/bots";
    public static String bot_name_path = bot_path+"/super";
    public static String aimlif_path = bot_path+"/aimlif";
    public static String aiml_path = bot_path+"/aiml";
    public static String config_path = bot_path+"/config";
    public static String log_path = bot_path+"/log";
    public static String sets_path = bot_path+"/sets";
    public static String maps_path = bot_path+"/maps";
}

