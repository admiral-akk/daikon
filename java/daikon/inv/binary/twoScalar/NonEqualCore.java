package daikon.inv.binary.twoScalar;

import daikon.*;
import daikon.inv.*;
import utilMDE.*;


public final class NonEqualCore implements java.io.Serializable {
  long min1 = Long.MAX_VALUE;
  long min2 = Long.MAX_VALUE;
  long max1 = Long.MIN_VALUE;
  long max2 = Long.MIN_VALUE;

  // If nonzero, use this as the range instead of the actual range.
  // This lets one use a specified probability of nonzero (say, 1/10
  // for pointers).
  long override_range = 0;

  int samples = 0;

  Invariant wrapper;

  // public NonEqualCore(Invariant wrapper) {
  //   this(wrapper, 0);
  // }

  public NonEqualCore(Invariant wrapper, long override_range) {
    this.wrapper = wrapper;
    this.override_range = override_range;
  }


  public void add_modified(long v1, long v2, int count) {
    if (wrapper.ppt.debugged) {
      System.out.println("NonEqual" + wrapper.ppt.varNames() + ".add_modified("
                         + v1 + "," + v2 + ", count=" + count + ")");
    }
    if (v1 == v2) {
      if (wrapper.ppt.debugged) {
        System.out.println("NonEqual.destroy()");
      }
      wrapper.destroy();
      return;
    }
    if (v1 < min1) min1 = v1;
    if (v1 > max1) max1 = v1;
    if (v2 < min2) min2 = v2;
    if (v2 > max2) max2 = v2;
    samples += count;
  }

  public double computeProbability() {
    if (wrapper.no_invariant)
      return Invariant.PROBABILITY_NEVER;
    else if ((min1 > max2) || (max1 < min2))
      return Invariant.PROBABILITY_UNKNOWN;
    else {
      double probability_one_nonequal;
      if (override_range != 0) {
        probability_one_nonequal = 1 - 1/(double)override_range;
      } else {
        long overlap = Math.min(max1, max2) - Math.max(min1, min2);
        // Looks like we're comparing pointers.  Fix this later.
        if (overlap < 0)
          return Invariant.PROBABILITY_JUSTIFIED;

        Assert.assert(overlap >= 0);
        overlap++;
        double range1 = (double)max1 - min1 + 1;
        double range2 = (double)max2 - min2 + 1;

        // probability of being equal by chance
        //  = (overlap/range1) * (overlap/range2) * (1/overlap)
        //  = overlap/(range1 * range2)

        // Hack; but this seems too stringent otherwise
        overlap *= 2;

        probability_one_nonequal = 1-((double)overlap)/(range1 * range2);
      }

      return Math.pow(probability_one_nonequal, samples);
    }
  }

  public String repr() {
    return "NonEqualCore: "
      + ",min1=" + min1
      + ",min2=" + min2
      + ",max1=" + max1
      + ",max2=" + max2;
  }


  public boolean isSameFormula(NonEqualCore other)
  {
    return true;
  }


  public boolean isExclusiveFormula(Invariant other)
  {
    return false;
  }

  public boolean isExact() {
    return false;
  }

}
