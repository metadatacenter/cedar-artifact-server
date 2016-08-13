package utils;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class TestParameterUtils {

  /**
   * Combines several lists of parameters to create permutations of all of them.
   * <p>
   * Example
   * Input: {{p1, p2}}, {{p3}, {p4}}
   * Output: {{p1, p2, p3}, {p1, p2, p4}}
   *
   * @param parameterLists
   * @return
   */
  public static Object[] getParameterPermutations(List<List<Object>> parameterLists) {
    if (parameterLists == null || parameterLists.isEmpty()) {
      return null;
    } else {
      List<List<Object>> result = new ArrayList<>();
      permutationsImpl(parameterLists, result, 0, new ArrayList<>());
      return toObjectArray(toFlatList(result));
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

  // Convert to flat list if necessary and return appropriate type
  private static List<List<String>> toFlatList(List<List<Object>> lists) {
    List<List<String>> result = new ArrayList<>();
    for (List<Object> l : lists) {
      List<String> r = new ArrayList();
      for (Object element : l) {
        if (element instanceof String) {
          r.add((String) element);
        } else if (element instanceof List) {
          r.addAll((List<String>) element);
        }
      }
      result.add(r);
    }
    return result;
  }

  private static Object[] toObjectArray(List<List<String>> lists) {
    List<String[]> output = new ArrayList<>();
    for (List<String> l : lists) {
      output.add(l.toArray(new String[0]));
    }
    return output.toArray();
  }

}