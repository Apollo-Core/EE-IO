package at.uibk.dps.ee.io.afcl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityCollections.CollectionOperation;

public class AfclCollectionOperationsTest {

  @Test
  public void testGetSubstrings() {
    String input = "1, 3, 1:3, src/name:3:src2/name";
    List<String> result =
        AfclCollectionOperations.getSubstrings(input, CollectionOperation.ElementIndex);
    assertEquals(7, result.size());
  }

  @Test
  public void testHasCollectionOperators() {
    DataIns in = new DataIns("input", "type");
    assertFalse(AfclCollectionOperations.hasCollectionOperations(in));
    PropertyConstraint c1 = new PropertyConstraint("c1", "whatever");
    List<PropertyConstraint> constraints = new ArrayList<>();
    in.setConstraints(constraints);
    constraints.add(c1);
    assertFalse(AfclCollectionOperations.hasCollectionOperations(in));
    PropertyConstraint c2 = new PropertyConstraint(ConstantsAfcl.constraintNameBlock, "whatever2");
    constraints.add(c2);
    assertTrue(AfclCollectionOperations.hasCollectionOperations(in));
    PropertyConstraint c3 =
        new PropertyConstraint(ConstantsAfcl.constraintNameElementIndex, "whatever3");
    constraints.add(c3);
    assertTrue(AfclCollectionOperations.hasCollectionOperations(in));
  }
}
