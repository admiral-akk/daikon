package daikon.inv.ternary.threeScalar;

import daikon.*;
import daikon.inv.*;

import utilMDE.*;

import java.util.*;


import java.util.logging.Logger;
import java.util.logging.Level;


public final class ThreeFloatFactory {

  /** Debug tracer. **/
  static final Logger debug = Logger.getLogger("daikon.inv.ternary.threeScalar.ThreeFloatFactory");


  public static final int max_instantiate
    =  ((FunctionsFloat.binarySymmetricFunctionNames.length
         * FunctionBinaryCoreFloat.order_symmetric_max)
        + (FunctionsFloat.binaryNonSymmetricFunctionNames.length
           * FunctionBinaryCoreFloat.order_nonsymmetric_max));

  // Add the appropriate new Invariant objects to the specified Invariants
  // collection.
  public static Vector instantiate(PptSlice ppt) {

    VarInfo var1 = ppt.var_infos[0];
    VarInfo var2 = ppt.var_infos[1];
    VarInfo var3 = ppt.var_infos[2];

    Assert.assertTrue((var1.rep_type == ProglangType.DOUBLE)
                  && (var2.rep_type == ProglangType.DOUBLE)
                  && (var3.rep_type == ProglangType.DOUBLE));

    if (debug.isLoggable(Level.FINE)) {
      debug.fine ("Instantiating for " + ppt.name());
      debug.fine ("Vars: " + var1.name + " " + var2.name + " " + var3.name);
    }

    if (! var1.compatible(var2)) {
      debug.fine ("Not comparable 1 to 2.  Returning");
      return null;
    }
    if (! var2.compatible(var3)) {
      debug.fine ("Not comparable 2 to 3.  Returning");
      return null;
    }
    // Check transitivity of "compatible" relationship.
    Assert.assertTrue(var1.compatible(var3));

    { // previously only if (pass == 2)
      // FIXME for equality
      Vector result = new Vector (FunctionBinaryFloat.instantiate_all (ppt));

      // Don't create LinearTernary if any of its variables are
      // constants.  DynamicConstants will create it from LinearBinary
      // and the constant value if/when all of its variables are non-constant
      PptTopLevel parent = ppt.parent;
      if (!parent.is_constant (var1) && !parent.is_constant(var2)
          && !parent.is_constant (var3))
        result.add(LinearTernaryFloat.instantiate(ppt));

      return (result);
    }
      /*

      for (int var_order = FunctionBinaryCoreFloat.order_symmetric_start;
           var_order <= FunctionBinaryCoreFloat.order_symmetric_max;
           var_order++) {
        for (int j=0; j<FunctionsFloat.binarySymmetricFunctionNames.length; j++) {
          // FunctionBinaryFloat fb = FunctionBinaryFloat.instantiate(ppt, FunctionsFloat.binarySymmetricFunctionNames[j], j, var_order);
           FunctionBinary fb = null;
          // no need to increment noninstantiated-invariants counters if
          // null; they were already incremented.
          if (fb != null) {
            result.add(fb);
          }
        }
      }
      for (int var_order = FunctionBinaryCoreFloat.order_nonsymmetric_start;
           var_order <= FunctionBinaryCoreFloat.order_nonsymmetric_max;
           var_order++) {
        for (int j=0; j<FunctionsFloat.binaryNonSymmetricFunctionNames.length; j++) {
          // FunctionBinaryFloat fb = FunctionBinaryFloat.instantiate(ppt, FunctionsFloat.binaryNonSymmetricFunctionNames[j], j+FunctionsFloat.binarySymmetricFunctionNames.length, var_order);
           FunctionBinary fb = null;
          // no need to increment noninstantiated-invariants counters if
          // null; they were already incremented.

          if (fb != null)
            result.add(fb);
        }
      }
      result.add(LinearTernaryFloat.instantiate(ppt));
      if (debug.isLoggable(Level.FINE)) {
        debug.fine ("Instantiated invs " + result);
      }
      return result;
    }
      */
  }

  private ThreeFloatFactory() {
  }

}
