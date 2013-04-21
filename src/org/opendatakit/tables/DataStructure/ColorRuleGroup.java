/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.DataStructure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.KeyValueHelper;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.util.Constants;

import android.util.Log;

/**
 * A ColorRuleGroup aggregates a collection of {@link ColorRule} objects and is
 * responsible for looking through the list of rules to determine the color
 * dictated by the collection.
 * @author sudar.sam@gmail.com
 *
 */
public class ColorRuleGroup {
  
  private static final String TAG = ColorRuleGroup.class.getName();
  
  /*****************************
   * Things needed for the key value store.
   *****************************/
  public static final String KVS_PARTITION_COLUMN = "ColumnColorRuleGroup";
  public static final String KEY_COLOR_RULES_COLUMN = 
      "ColumnColorRuleGroup.ruleList";
  public static final String KVS_PARTITION_TABLE = "TableColorRuleGroup";
  public static final String KEY_COLOR_RULES_TABLE = 
      "TableColorRuleGroup.ruleList";
  public static final String DEFAULT_KEY_COLOR_RULES = "[]";
  
  private final TableProperties tp;
  private final ColumnProperties cp;
  // this remains its own field (which must always match cp.getElementKey())
  // b/c it is easier for the caller to just pass in the elementKey, and the 
  // code currently uses null to mean "don't get me a color ruler."
  private final String elementKey;
  private final ObjectMapper mapper;
  private final TypeFactory typeFactory;
  private final KeyValueStoreHelper kvsh;
  private final KeyValueHelper aspectHelper;
  // This is the list of actual rules that make up the ruler.
  private List<ColorRule> ruleList;
  
  /**
   * Construct the rule group for the given column. 
   * @param tp
   * @param elementKey
   */
  private ColorRuleGroup(TableProperties tp, String elementKey) {
    this.tp = tp;
    this.kvsh = tp.getKeyValueStoreHelper(KVS_PARTITION_COLUMN);
    this.aspectHelper = kvsh.getAspectHelper(elementKey);
    this.elementKey = elementKey;
    this.mapper = new ObjectMapper();
    this.typeFactory = mapper.getTypeFactory();
    mapper.setVisibilityChecker(
        mapper.getVisibilityChecker().withFieldVisibility(Visibility.ANY));
    mapper.setVisibilityChecker(
        mapper.getVisibilityChecker()
        .withCreatorVisibility(Visibility.ANY));
    if (elementKey == null) {
      this.cp = null;
    } else {
      this.cp = tp.getColumnByElementKey(elementKey);
    }
    String jsonRulesString = 
        aspectHelper.getObject(KEY_COLOR_RULES_COLUMN);
    this.ruleList = parseJsonString(jsonRulesString);
  }
  
  /**
   * Construct the rule group for the given table.
   * @param tp
   */
  private ColorRuleGroup(TableProperties tp) {
    this.tp = tp;
    this.kvsh = tp.getKeyValueStoreHelper(KVS_PARTITION_TABLE);
    this.aspectHelper = null;
    this.elementKey = null;
    this.mapper = new ObjectMapper();
    this.typeFactory = mapper.getTypeFactory();
    mapper.setVisibilityChecker(
        mapper.getVisibilityChecker().withFieldVisibility(Visibility.ANY));
    mapper.setVisibilityChecker(
        mapper.getVisibilityChecker()
        .withCreatorVisibility(Visibility.ANY));
    if (elementKey == null) {
      this.cp = null;
    } else {
      this.cp = tp.getColumnByElementKey(elementKey);
    }
    String jsonRulesString = 
        kvsh.getObject(KEY_COLOR_RULES_TABLE);
    this.ruleList = parseJsonString(jsonRulesString);
  }
    
    public static ColorRuleGroup getColumnColorRuler(TableProperties tp,
        String elementKey) {
      return new ColorRuleGroup(tp, elementKey);
    }
    
    public static ColorRuleGroup getTableColorRuleGroup(TableProperties tp) {
      return new ColorRuleGroup(tp);
    }
    
    /**
     * Parse a json String of a list of {@link ColorRule} objects into a 
     * @param json
     * @return
     */
    private List<ColorRule> parseJsonString(String json) {
      if (json == null || json.equals("")) { // no values in the kvs
        return new ArrayList<ColorRule>();
      }
      List<ColorRule> reclaimedRules = new ArrayList<ColorRule>();
      try {
        reclaimedRules = 
            mapper.readValue(json, 
                typeFactory.constructCollectionType(ArrayList.class, 
                    ColorRule.class));
      } catch (JsonParseException e) {
        Log.e(TAG, "problem parsing json to colcolorrule");
        e.printStackTrace();
      } catch (JsonMappingException e) {
        Log.e(TAG, "problem mapping json to colcolorrule");
        e.printStackTrace();
      } catch (IOException e) {
        Log.e(TAG, "i/o problem with json to colcolorrule");
        e.printStackTrace();
      }
      return reclaimedRules;
    }
    
    /**
     * Return the list of rules that makes up this column. This should only be
     * used for displaying the rules. Any changes to the list should be made
     * via the add, delete, and update methods in ColumnColorRuler.
     * @return
     */
    public List<ColorRule> getColorRules() {
      return ruleList;
    }
    
    /**
     * Replace the list of rules that define this ColumnColorRuler.
     * @param newRules
     */
    public void replaceColorRuleList(List<ColorRule> newRules) {
      this.ruleList = newRules;
    }
    
    /**
     * Persist the rule list into the key value store. Does nothing if there are
     * no rules, so will not pollute the key value store unless something has
     * been added.
     */
    public void saveRuleList() {
      // if there are no rules, we want to remove the key from the kvs.
      if (ruleList.size() == 0) {
        if (elementKey != null) {
          aspectHelper.removeKey(KEY_COLOR_RULES_COLUMN);
        } else {
          // We know that it's actually a table rule.
          kvsh.removeKey(KEY_COLOR_RULES_TABLE);
        }
        return;
      }
      // set it to this default just in case something goes wrong and it is 
      // somehow set. this way if you manage to set the object you will have
      // something that doesn't throw an error when you expect to get back 
      // an array list. it will just be of length 0. not sure if this is a good
      // idea or not.
      String ruleListJson = DEFAULT_KEY_COLOR_RULES;
      try {
        ruleListJson = mapper.writeValueAsString(ruleList);
        // The elementKey here is kind of standing in as a flag for whether 
        // it's a table or a column rule.
        if (elementKey != null) {
          aspectHelper.setObject(KEY_COLOR_RULES_COLUMN, ruleListJson);
        } else {
          kvsh.setObject(KEY_COLOR_RULES_TABLE, ruleListJson);
        }
      } catch (JsonGenerationException e) {
        Log.e(TAG, "problem parsing list of color rules");
        e.printStackTrace();
      } catch (JsonMappingException e) {
        Log.e(TAG, "problem mapping list of color rules");
        e.printStackTrace();
      } catch (IOException e) {
        Log.e(TAG, "i/o problem with json list of color rules");
        e.printStackTrace();
      }
    }
    
    private void addRule(ColorRule rule) {
      ruleList.add(rule);
    }
    
    /**
     * Replace the rule matching updatedRule's id with updatedRule.
     * @param updatedRule
     */
    public void updateRule(ColorRule updatedRule) {
      for (int i = 0; i < ruleList.size(); i++) {
        if (ruleList.get(i).getRuleId().equals(updatedRule.getRuleId())) {
          ruleList.set(i, updatedRule);
          return;
        }
      }
      Log.e(TAG, "tried to update a rule that matched no saved ids");
    }
    
    /**
     * Remove the given rule from the rule list.
     * @param rule
     */
    public void removeRule(ColorRule rule) {
      for (int i = 0; i < ruleList.size(); i++) {
        if (ruleList.get(i).getRuleId().equals(rule.getRuleId())) {
          ruleList.remove(i);
          return;
        }
      }
      Log.d(TAG, "a rule was passed to deleteRule that did not match" +
           " the id of any rules in the list");
    }
    
    public int getRuleCount() {
      return ruleList.size();
    }
    
    /**
     * Use the rule group to determine if it applies to the given data. 
     * @param rowData an array of data from the row
     * @param indexMapping a mapping of element key to index in the rowData
     * array
     * @param propertiesMapping a mapping of element key to 
     * {@link ColumnProperties}. Necessary for knowing how to interpret the 
     * row data (int, number, String, etc).
     * @return If there was a matching rule in the group, {@link ColorGuide} 
     * with didMatch set to true and the appropriate foreground and background 
     * colors set to the row. Otherwise, the {@link ColorGuide} will have 
     * didMatch return false and meaningless default values set to the
     * foreground and background colors.
     * 
     */
    public ColorGuide getColorGuide(String[] rowData, 
        Map<String, Integer> indexMapping, 
        Map<String, ColumnProperties> propertiesMapping) {
      for (int i = 0; i < ruleList.size(); i++) {
        if (ruleList.get(i).checkMatch(rowData, indexMapping, 
            propertiesMapping)) {
          return new ColorGuide(true, ruleList.get(i).getForeground(),
              ruleList.get(i).getBackground());
        }
      }
      return new ColorGuide(false, Constants.DEFAULT_TEXT_COLOR,
          Constants.DEFAULT_BACKGROUND_COLOR);
    }
    
    /**
     * Class for interpreting the result of a test of the rule group. When this
     * is returned you are able to distinguish via the {@link didMatch} method 
     * whether or not the rule should apply.
     * @author sudar.sam@gmail.com
     *
     */
    public class ColorGuide {
      
      private int mForeground;
      private int mBackground;
      private boolean mMatched;
      
      public ColorGuide(boolean matched, int foreground, int background) {
        this.mMatched = matched;
        this.mForeground = foreground;
        this.mBackground = background;
      }
      
      public boolean didMatch() {
        return mMatched;
      }
      
      public int getForeground() {
        return mForeground;
      }
      
      public int getBackground() {
        return mBackground;
      }
    }
    
}