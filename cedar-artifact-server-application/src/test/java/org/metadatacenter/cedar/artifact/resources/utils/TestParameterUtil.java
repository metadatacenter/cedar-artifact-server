package org.metadatacenter.cedar.artifact.resources.utils;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class TestParameterUtil {

  /**
   * Combines several lists of parameters to create permutations of all of them.
   *
   * Example
   * Input: {{p1, p2}}, {{p3}, {p4}}
   * Output: {{p1, p2, p3}, {p1, p2, p4}}
   *
   * @param parameterLists
   * @return
   */
  public static Object[] getParameterPermutations(List<List<Object>> parameterLists) {
    if (parameterLists == null || parameterLists.isEmpty()) {
      // empty array
      return new Object[0];
    } else {
      List<List<Object>> result = new ArrayList<>();
      permutationsImpl(parameterLists, result, 0, new ArrayList<>());
      List<List<Object>> resultFlatLists = toFlatLists(result);
      Object[] resultArray = toObjectArray(resultFlatLists);
      return resultArray;
    }
  }

  private static void permutationsImpl(List<List<Object>> original, List<List<Object>> result, int count, List<Object> current) {
    // Final
    if (count == original.size()) {
      result.add(current);
      return;
    }
    // Iterate from current collection and copy 'current' element N times, one for each element
    List<Object> currentCollection = original.get(count);
    for (Object element : currentCollection) {
      List<Object> copy = Lists.newArrayList(current);
      copy.add(element);
      permutationsImpl(original, result, count + 1, copy);
    }
  }

  /**
   * If necessary, converts List<List<List<Object>>> to List<List<Object>>
   */
  private static List<List<Object>> toFlatLists(List<List<Object>> lists) {
    List<List<Object>> result = new ArrayList<>();
    for (List<Object> list : lists) {
      List<Object> r = new ArrayList();
      for (Object element : list) {
        if (element instanceof List) {
          r.addAll((List<Object>) element);
        }
        else {
          r.add(element);
        }
      }
      result.add(r);
    }
    return result;
  }

  private static Object[] toObjectArray(List<List<Object>> lists) {
    List<Object[]> output = new ArrayList<>();
    for (List<Object> l : lists) {
      output.add(l.toArray(new Object[0]));
    }
    return output.toArray();
  }


}
