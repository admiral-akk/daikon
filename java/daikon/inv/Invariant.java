package daikon.inv;

import daikon.*;

import java.util.*;

import utilMDE.*;

// Base implementation for Invariant objects.
// Intended to be subclassed but not to be directly instantiated.
// I should probably rename this to "Invariant" and get rid of that interface.o

public abstract class Invariant implements java.io.Serializable {

  static boolean debug_isWorthPrinting = false;

  public PptSlice ppt;      // includes values, number of samples, VarInfos, etc.

  // Has to be public so wrappers can read it.
  /**
   * True exactly if the invariant is guaranteed never to hold (and should
   * be either in the process of being destroyed or about to be
   * destroyed.  This should never be set directly; instead, call destroy().
   **/
  public boolean no_invariant = false;

  // True if we've seen all values and should ignore further add() methods.
  // This is rather a hack and should be removed later.
  // Actually, it's not used any longer, except to be checked in assertions.
  public boolean finished = false;

  // Subclasses should set these; Invariant never does.

  // The probability that this could have happened by chance alone.
  //   0 = could never have happened by chance; that is, we are fully confident
  //       that this invariant is a real invariant
  public final static double PROBABILITY_JUSTIFIED = 0;
  //   (0..1) = greater to lesser likelihood of coincidence
  //   1 = must have happened by chance
  public final static double PROBABILITY_UNJUSTIFIED = 1;
  //   2 = we haven't yet seen enough data to know whether this invariant is
  //       true, much less its justification
  public final static double PROBABILITY_UNKNOWN = 2;
  //   3 = delete this invariant; we know it's not true
  public final static double PROBABILITY_NEVER = 3;

  // The probability that the invariant occurred by chance must be less
  // than this in order for it to be displayed.
  public static double probability_limit = .01;

  /**
   * At least this many samples are required, or else we don't report any
   * invariant at all.  (Except that OneOf invariants are treated differently.)
   **/
  public final static int min_mod_non_missing_samples = 5;

  public boolean enoughSamples() {
    return true;
  }

  // If probability == PROBABILITY_NEVER, then this invariant can be eliminated.
  /**
   * Given that this invariant has been true for all values seen so far,
   * this method returns the probability that that situation has occurred
   * by chance alone.  The result is a value between 0 and 1 inclusive.  0
   * means that this invariant could never have occurred by chance alone;
   * we are fully confident that its truth is no coincidence.  1 means that
   * the invariant is certainly a happenstance, so the truth of the
   * invariant is not relevant and it should not be reported.  Values
   * between 0 and 1 give differing confidences in the invariant.
   * <p>
   *
   * As an example, if the invariant is "x!=0", and only one value, 22, has
   * been seen for x, then the conclusion "x!=0" is not justified.  But if
   * there have been 1,000,000 values, and none of them were 0, then we may
   * be confident that the property "x!=0" is not a coincidence.
   * <p>
   *
   * This method need not check the value of variable no_invariant, as the
   * caller does that.
   **/
  public double getProbability() {
    if (no_invariant)
      return PROBABILITY_NEVER;
    double result = computeProbability();
    if (result > PROBABILITY_NEVER) {
      // Can't print this.repr_prob(), as it may compute the probability!
      System.out.println("Bad invariant probability " + result + ": ");
      System.out.println(this.getClass());
      System.out.println(repr());
      System.out.println(this.format());
    }
    // System.out.println("getProbability: " + getClass().getName() + " " + ppt.varNames());
    Assert.assert((result == PROBABILITY_JUSTIFIED)
		  || (result == PROBABILITY_UNJUSTIFIED)
		  || (result == PROBABILITY_UNKNOWN)
		  || (result == PROBABILITY_NEVER)
		  || ((0 <= result) && (result <= 1))
                  // This can be expensive, so comment out.
                  // , getClass().getName() + ": " + repr()
                  );
    return result;
  }

  /**
   * This method computes the probability that this invariant occurred by chance.
   * Users should use getProbability() instead.
   * @see     getProbability()
   **/
  protected abstract double computeProbability();

  public boolean justified() {
    return (!no_invariant) && enoughSamples() && (getProbability() <= probability_limit);
  }

  /**
   * Subclasses should override.  An exact invariant indicates taht given
   * all but one variable value, the last one can be computed.  (I think
   * that's correct, anyway.)  Examples are IntComparison (when only
   * equality is possible), LinearBinary, FunctionUnary.
   * OneOf is treated differently, as an interface.
   * The result of this method does not depend on whether the invariant is
   * justified, destroyed, etc.
   **/
  public boolean isExact() {
    return false;
  }

  // Implementations of this need to examine all the data values already
  // in the ppt.  Or, don't put too much work in the constructor and instead
  // have the caller do that.
  protected Invariant(PptSlice ppt) {
    this.ppt = ppt;
    // We don't want to add the invariant yet, as this constructor is
    // called from the constructors for subclasses of Invariant.
    //     if (Global.debugInfer)
    //       System.out.println("Adding invariant " + this + " to Ppt " + ppt.name + " = " + ppt + "; now has " + ppt.invs.size() + " invariants in " + ppt.invs);
    //     ppt.addInvariant(this);
    //     if (Global.debugInfer)
    //       System.out.println("Added invariant " + this + " to Ppt " + ppt.name + " = " + ppt + "; now has " + ppt.invs.size() + " invariants in " + ppt.invs);
  }

  // Has to be public because of wrappers.
  public void destroy() {
    no_invariant = true;
    ppt.removeInvariant(this);
  }

  // Regrettably, I can't declare a static abstract method.
  // // The return value is probably ignored.  The new Invariant installs
  // // itself on the PptSlice, and that's what really matters (right?).
  // public static abstract Invariant instantiate(PptSlice ppt);

  public boolean usesVar(VarInfo vi) {
    return ppt.usesVar(vi);
  }

  public boolean usesVar(String name) {
    return ppt.usesVar(name);
  }

  // Not used as of 1/31/2000.
  // // For use by subclasses.
  // /** Put a string representation of the variable names in the StringBuffer. */
  // public void varNames(StringBuffer sb) {
  //   // sb.append(this.getClass().getName());
  //   ppt.varNames(sb);
  // }

  /** Return a string representation of the variable names. */
  final public String varNames() {
    return ppt.varNames();
  }

  final public String name() {
    return this.getClass().getName() + varNames();
  }

  // Should not include result of getProbability because this may be called
  // from computeProbability or elsewhere for debugging purposes.
  /**
   * For printing invariants, there are two interfaces:
   * repr gives a low-level representation
   * (repr_prop also prints the probability), and
   * format gives a high-level representation for user output.
   **/
  public String repr() {
    // A better default would be to use reflection and print out all
    // the variable names.
    return getClass() + varNames() + ": " + format();
  }

  /**
   * For printing invariants, there are two interfaces:
   * repr gives a low-level representation
   * (repr_prop also prints the probability), and
   * format gives a high-level representation for user output.
   **/
  public String repr_prob() {
    return repr() + "; probability = " + getProbability();
  }

  /**
   * For printing invariants, there are two interfaces:
   * repr gives a low-level representation
   * (repr_prop also prints the probability), and
   * format gives a high-level representation for user output.
   **/
  public abstract String format();

  /**
   * ESC-like representation.
   **/
  public String format_esc() {
    return "format_esc needs to be changed: " + format();
  }

  /**
   * Representation for the Simplify theorem prover.
   **/
  public String format_simplify() {
    return "format_simplify needs to be changed: " + format();
  }

  /**
   * IOA Representation
   **/
  public String format_ioa(String classname) {
    return "format_ioa needs to be changed: " + format();
  }

  // This should perhaps be merged with some kind of PptSlice comparator.
  /**
   * Compare based on arity, then printed representation.
   **/
  public static final class InvariantComparatorForPrinting implements Comparator {
    public int compare(Object o1, Object o2) {
      if (o1 == o2)
	return 0;
      Invariant inv1 = (Invariant)o1;
      Invariant inv2 = (Invariant)o2;
      // Assert.assert(inv1.ppt.parent == inv2.ppt.parent);
      VarInfo[] vis1 = inv1.ppt.var_infos;
      VarInfo[] vis2 = inv2.ppt.var_infos;
      int arity_cmp = vis1.length - vis2.length;
      if (arity_cmp != 0)
	return arity_cmp;
      // Comparing on variable index is wrong in general:  variables of the
      // same name may have different indices at different program points.
      // However, it's safe if the invariants are from the same program
      // point.  Also, it is nice to avoid changing the order of variables
      // from that of the data trace file.

      if (inv1.ppt.parent == inv2.ppt.parent) {
        for (int i=0; i<vis1.length; i++) {
          int tmp = vis1[i].varinfo_index - vis2[i].varinfo_index;
          if (tmp != 0) {
            // This can happen when variable names have been changed by
            // VarInfo.simplify_expression().  For now, hope for the best.
            // (That is, hope it doesn't produce multiple invariants or
            // confused formatting.)
            // if (inv1.format().equals(inv2.format())) {
            //   System.out.println("ICFP says different, but same formatting:");
            //   System.out.println("  " + inv1.format() + " " + inv1.repr() + " at " + inv1.ppt.name);
            //   System.out.println(" var #" + vis1[i].varinfo_index + " = " + vis1[i].name + " = " + vis1[i]);
            //   System.out.println("  " + inv2.format() + " " + inv2.repr() + " at " + inv2.ppt.name);
            //   System.out.println(" var #" + vis2[i].varinfo_index + " = " + vis2[i].name + " = " + vis2[i]);
            // }

            // // Debugging
            // System.out.println("ICFP: compare var "
            //                    + vis1[i].name.name() + " (index " + vis1[i].varinfo_index + ")"
            //                    + " to " + vis2[i].name.name() + " (index " + vis2[i].varinfo_index + ")"
            //                    + " => " + tmp
            //                    + "\t\tfor " + inv1.format() + ", " + inv2.format());
            // System.out.println("Vars for " + inv1.format() + ": " + inv1.repr());
            // System.out.println("Vars for " + inv2.format() + ": " + inv2.repr());

            return tmp;
          }
        }
      } else {
        // // Debugging
        // System.out.println("ICFP: different parents for " + inv1.format() + ", " + inv2.format());

        for (int i=0; i<vis1.length; i++) {
          String name1 = vis1[i].name.name();
          String name2 = vis2[i].name.name();
          if (name1.equals(name2)) {
            continue;
          }
          int name1in2 = ((PptTopLevel)inv2.ppt.parent).indexOf(name1);
          int name2in1 = ((PptTopLevel)inv1.ppt.parent).indexOf(name2);
          int cmp1 = (name1in2 == -1) ? 0 : vis1[i].varinfo_index - name1in2;
          int cmp2 = (name2in1 == -1) ? 0 : vis2[i].varinfo_index - name2in1;
          int cmp = MathMDE.sign(cmp1) + MathMDE.sign(cmp2);
          if (cmp != 0)
            return cmp;
        }
      }

      // System.out.println("ICFP: default rule yields "
      //                    + inv1.format().compareTo(inv2.format())
      //                    + " for " + inv1.format() + ", " + inv2.format());
      return inv1.format().compareTo(inv2.format());
    }
  }

  /**
   * @return true iff the two invariants represent the same
   * mathematical formula.  Does not consider the context such as
   * variable names, confidences, sample counts, value counts, or
   * related quantities.  As a rule of thumb, if two invariants format
   * the same, this method returns true.  Furthermore, in many cases,
   * if an invariant does not involve computed constants (as "x&gt;c" and
   * "y=ax+b" do for constants a, b, and c), then this method vacuously
   * returns true.
   *
   * @exception RuntimeException if other.class != this.class
   **/
  public boolean isSameFormula(Invariant other) {
    return false;
  }

  public static interface IsSameInvariantNameExtractor
  {
    public VarInfoName getFromFirst(VarInfo var1);
    public VarInfoName getFromSecond(VarInfo var2);
  }

  public static class DefaultIsSameInvariantNameExtractor
    implements IsSameInvariantNameExtractor
  {
    public VarInfoName getFromFirst(VarInfo var1)  { return var1.name; }
    public VarInfoName getFromSecond(VarInfo var2) { return var2.name; }
  }
  private static final IsSameInvariantNameExtractor defaultIsSameInvariantNameExtractor = new DefaultIsSameInvariantNameExtractor();

  /**
   * @return true iff the argument is the "same" invariant as this.
   * Same, in this case, means a matching type, formula, and variable
   * names.
   **/
  public boolean isSameInvariant(Invariant inv2)
  {
    return isSameInvariant(inv2, defaultIsSameInvariantNameExtractor);
  }

  /**
   * @param name_extractor lambda to extract the variable name from the VarInfos
   * @return true iff the argument is the "same" invariant as this.
   * Same, in this case, means a matching type, formula, and variable
   * names.
   **/
  public boolean isSameInvariant(Invariant inv2,
				 IsSameInvariantNameExtractor name_extractor)
  {
    Invariant inv1 = this;

    // Can't be the same if they aren't the same type
    if (!inv1.getClass().equals(inv2.getClass())) {
      return false;
    }

    // Can't be the same if they aren't the same formula
    if (!inv1.isSameFormula(inv2)) {
      return false;
    }

    // System.out.println("isSameInvariant(" + inv1.format() + ", " + inv2.format() + ")");

    // The variable names much match up, in order

    VarInfo[] vars1 = inv1.ppt.var_infos;
    VarInfo[] vars2 = inv2.ppt.var_infos;

    Assert.assert(vars1.length == vars2.length); // due to inv type match already
    for (int i=0; i < vars1.length; i++) {
      VarInfo var1 = vars1[i];
      VarInfo var2 = vars2[i];

      // Do the easy check first
      if (name_extractor.getFromFirst(var1).equals(name_extractor.getFromSecond(var2))) {
	continue;
      }

      // The names "match" iff there is an intersection of the names
      // of equal variables.
      Vector all_vars1 = var1.canonicalRep().equalTo();
      Vector all_vars2 = var2.canonicalRep().equalTo();
      all_vars1.add(var1.canonicalRep());
      all_vars2.add(var2.canonicalRep());
      Vector all_vars_names1 = new Vector(all_vars1.size());
      for (Iterator iter = all_vars1.iterator(); iter.hasNext(); ) {
	VarInfo elt = (VarInfo) iter.next();
	VarInfoName name = name_extractor.getFromFirst(elt);
	all_vars_names1.add(name);
      }
      boolean intersection = false;
      for (Iterator iter = all_vars2.iterator(); iter.hasNext(); ) {
	VarInfo elt = (VarInfo) iter.next();
	VarInfoName name = name_extractor.getFromSecond(elt);
	intersection = all_vars_names1.contains(name);
	if (intersection) {
	  break;
	}
      }
      if (!intersection) {
	return false;
      }
    }

    // System.out.println("TRUE: isSameInvariant(" + inv1.format() + ", " + inv2.format() + ")");

    // the type, formula, and vars all matched
    return true;
  }


  /**
   * @return true iff the two invariants represent mutually exclusive
   * mathematical formulas -- that is, if one of them is true, then the
   * other must be false.  This method does not consider the context such
   * as variable names, confidences, sample counts, value counts, or
   * related quantities.
   **/
  public boolean isExclusiveFormula(Invariant other) {
    return false;
  }


  // This is a little grody; stick with code cut-and-paste for now.
  // // Look up a previously instantiated Invariant.
  // // Should this implementation be made more efficient?
  // public static Invariant find(Class invclass, PptSlice ppt) {
  //   for (Iterator itor = ppt.invs.iterator(); itor.hasNext(); ) {
  //     Invariant inv = (Invariant) itor.next();
  //     if (inv instanceof invclass)
  //       return inv;
  //   }
  //   return null;
  // }



  // Diff replaced by package daikon.diff

  //    String diff(Invariant other) {
  //      throw new Error("Unimplemented invariant diff for " + this.getClass() + " and " + other.getClass() + ": " + this.format() + " " + other.format());
  //    }

  //     # Possibly add an optional "args=None" argument, for formatting.
  //     def diff(self, other):
  //         """Returns None or a description of the difference."""
  //         # print "diff(invariant)"
  //         inv1 = self
  //         inv2 = other
  //         assert inv1.__class__ == inv2.__class__
  //         if inv1.is_unconstrained() and inv2.is_unconstrained():
  //             return None
  //         if inv1.is_unconstrained() ^ inv2.is_unconstrained():
  //             return "One is unconstrained but the other is not"
  //         if inv1.one_of and inv2.one_of and inv1.one_of != inv2.one_of:
  //             return "Different small number of values"
  //         if inv1.can_be_None ^ inv2.can_be_None:
  //             return "One can be None but the other cannot"
  //         # return "invariant.diff: no differences"  # debugging
  //         return None



  ///////////////////////////////////////////////////////////////////////////
  /// Tests about the invariant (for printing)
  ///

  public final boolean isWorthPrinting()
  {
    if (debug_isWorthPrinting) {
      System.out.println("isWorthPrinting: " + format() + " at " + ppt.name);
    }

    // It's hard to know in exactly what order to do these checks that
    // eliminate some invariants from consideration.  Which is cheapest?
    // Which is most often successful?  Which assume others have already
    // been performed?
    if (! isWorthPrinting_sansControlledCheck()) {
      if (debug_isWorthPrinting) {
        System.out.println("  not worth printing, sans controlled check: " + format() + " at " + ppt.name);
      }
      return false;
    }

    // The invariant is worth printing on its own merits, but it may be
    // controlled.  If any (transitive) controller is worth printing, don't
    // print this one.
    // Use _sorted version for reproducibility.  (There's a bug, but I can't find it.)
    Vector contr_invs = find_controlling_invariants_sorted();
    Vector processed = new Vector();
    while (contr_invs.size() > 0) {
      Invariant contr_inv = (Invariant) contr_invs.remove(0);
      if (debug_isWorthPrinting) {
        System.out.println("Controller " + contr_inv.format() + " at " + contr_inv.ppt.name + " for: " + format() + " at " + ppt.name);
      }

      processed.add(contr_inv);
      if (contr_inv.isWorthPrinting_sansControlledCheck()) {
	// we have a printable controller, so we shouldn't print
        if (debug_isWorthPrinting) {
          System.out.println("  not worth printing, sans controlled check, due to controller " + contr_inv.format() + " at " + contr_inv.ppt.name + ": " + format() + " at " + ppt.name);
        }
        return false;
      }
      // find the controlling invs of contr_inv and add them to the
      // working set iff the are not already in it and they have not
      // been processed already
      Iterator iter = contr_inv.find_controlling_invariants().iterator();
      while (iter.hasNext()) {
	Object elt = iter.next();
	if (!processed.contains(elt) && !contr_invs.contains(elt)) {
	  contr_invs.add(elt);
	}
      }
    }

    // No controller was worth printing
    if (debug_isWorthPrinting) {
      System.out.println("isWorthPrinting => true for: " + format() + " at " + ppt.name);
    }
    return true;
  }


  /**
   * Like isWorthPrinting, but doesn't check whether the invariant is controlled.
   **/
  final public boolean isWorthPrinting_sansControlledCheck() {
    if (this instanceof Implication) {
      Implication impl = (Implication) this;
      if (debug_isWorthPrinting) {
        System.out.println("iwpscc(" + format() + ") dispatching");
      }
      return impl.predicate.isWorthPrinting() && impl.consequent.isWorthPrinting();
    }

    if (debug_isWorthPrinting) {
      System.out.println(isWorthPrinting_sansControlledCheck_debug());
    }
    boolean result
      = ((! hasFewModifiedSamples())
         && enoughSamples()       // perhaps replaces hasFewModifiedSamples
         && (! hasNonCanonicalVariable())
         && (! hasOnlyConstantVariables())
         && (! isObvious())
	 && justified()
         && isWorthPrinting_PostconditionPrestate());
    return result;
  }

  final public String isWorthPrinting_sansControlledCheck_debug() {
    return
      "iwpscc(" + format() + " @ " + ppt.name
      + ") <= " + (! hasFewModifiedSamples())
      + " " + enoughSamples()
      + " " + (! hasNonCanonicalVariable())
      + " " + (! hasOnlyConstantVariables())
      + " " + (! isObvious())
      + " " + justified()
      + " " + isWorthPrinting_PostconditionPrestate();
  }

  /**
   * @return true if this invariant has few modified (non-repeated) samples.
   * An exception is made for OneOf invariants.
   **/
  public final boolean hasFewModifiedSamples() {
    int num_mod_non_missing_samples = ppt.num_mod_non_missing_samples();

    if (this instanceof OneOf) {
      // A OneOf should have at least as many samples as it has values.
      // Was an assert...
      if (((OneOf) this).num_elts() > num_mod_non_missing_samples) {
        System.out.println("OneOf problem: num_elts " + ((OneOf) this).num_elts() + ", num_mod " + num_mod_non_missing_samples + ": " + format());
      }
      return false;
    } else {
      boolean result = (num_mod_non_missing_samples < Invariant.min_mod_non_missing_samples);
      // if (! result) {
      //   System.out.println("hasFewModifiedSamples: " + format());
      // }
      return result;
    }
  }

  // This used to be final, but I want to override in EqualityInvariant
  /** @return true if this invariant involves a non-canonical variable **/
  public boolean hasNonCanonicalVariable() {
    VarInfo[] vis = ppt.var_infos;
    for (int i=0; i<vis.length; i++) {
      if (! vis[i].isCanonical()) {
        return true;
      }
    }
    return false;
  }


  /**
   * @return true if this invariant involves only constant variables
   *         and is a comparison
   **/
  public boolean hasOnlyConstantVariables() {
    VarInfo[] varInfos = ppt.var_infos;
    for (int i=0; i < varInfos.length; i++) {
      if (! varInfos[i].isConstant())
	return false;
    }

    // At this point, we know all variables are constant.
    Assert.assert(this instanceof OneOf ||
		  this instanceof Comparison ||
		  this instanceof Equality
		  , "Unexpected invariant with all vars constant: "
		  + this + "  " + repr_prob() + "  " + format()
		  );
    if (this instanceof Comparison) {
      //      Assert.assert(! IsEqualityComparison.it.accept(this));
      if (Global.debugPrintInvariants)
	System.out.println("  [over constants:  " + this.repr_prob() + " ]");
      return true;
    }
    return false;
  }

  /**
   * @return true if this invariant is necessarily true, due to derived
   * variables, other invariants, etc.
   * Intended to be overridden by subclasses.
   **/
  public final boolean isObvious() {
    // Actually actually, we'll eliminate invariants as they become obvious
    // rather than on output; the point of this is to speed up computation.
    // // Actually, we do need to check isObviousDerived after all because we
    // // add invariants that might be obvious, but might also turn out to be
    // // even stronger (and so not obvious).  We don't know how the invariant
    // // turns out until after testing it.
    // // // We don't need to check isObviousDerived because we won't add
    // // // obvious-derived invariants to lists in the first place.
    if (isObviousDerived() || isObviousImplied()) {
      if (Global.debugPrintInvariants)
	System.out.println("  [obvious:  " + repr_prob() + " ]");
      return true;
    }
    return false;
  }

  /**
   * @return true if this invariant is necessarily true, due to being implied
   * by other (more basic or preferable to report) invariants.
   * Intended to be overridden by subclasses.
   **/
  public boolean isObviousDerived() {
    return false;
  }

  /**
   * @return true if this invariant is necessarily true, due to being implied
   * by other (more basic or preferable to report) invariants.
   * Intended to be overridden by subclasses.
   **/
  public boolean isObviousImplied() {
    return false;
  }

  /**
   * @return true if this invariant is controlled by another invariant
   **/
  public boolean isControlled() {
    Vector controllers = this.find_controlling_invariants();
    return (controllers.size() > 0);
  }

  /**
   * @return true if this invariant is a postcondition that is implied
   * by prestate invariants.  For example, if an entry point has the
   * invariant x+3=y, and this invariant is the corresponding exit
   * point invariant orig(x)+3=orig(y), then this methods returns
   * true.
   **/
  public boolean isImpliedPostcondition() {
    PptTopLevel topLevel = (PptTopLevel) ppt.parent;
    if (topLevel.entry_ppt() != null) { // if this is an exit point invariant
      Iterator entryInvariants = topLevel.entry_ppt().invariants_vector().iterator(); // unstable
      while (entryInvariants.hasNext()) {
	Invariant entryInvariant = (Invariant) entryInvariants.next();
	// If entryInvariant with orig() applied to everything matches this invariant
	if (entryInvariant.isSameInvariant( this, preToPostIsSameInvariantNameExtractor))
	  return true;
      }
    }
    return false;
  }

  private boolean isWorthPrinting_PostconditionPrestate()
  {
    PptTopLevel pptt = (PptTopLevel) ppt.parent;

    if (Daikon.suppress_implied_postcondition_over_prestate_invariants) {
      if (pptt.entry_ppt != null) {
	Iterator entry_invs = pptt.entry_ppt.invariants_iterator(); // unstable
	while (entry_invs.hasNext()) {
	  Invariant entry_inv = (Invariant) entry_invs.next();
	  // If entry_inv with orig() applied to everything matches this
	  if (entry_inv.isSameInvariant(this, preToPostIsSameInvariantNameExtractor)) {
	    if (entry_inv.isWorthPrinting_sansControlledCheck()) {
              if (debug_isWorthPrinting) {
                System.out.println("isWorthPrinting_PostconditionPrestate => false for " + format());
              }
	      return false;
	    }
	  }
	}
      }
    }
    return true;
  }

  /**
   * Used in isImpliedPostcondition() and isWorthPrinting_PostconditionPrestate().
   **/
  private final static IsSameInvariantNameExtractor preToPostIsSameInvariantNameExtractor =
    new DefaultIsSameInvariantNameExtractor() {
	public VarInfoName getFromFirst(VarInfo var)
	{ return super.getFromFirst(var).applyPrestate(); }
      };

  /**
   * Returns a Vector[Invariant] which are the sameInvariant as this,
   * drawn from the invariants of this.ppt.parent.controllers.
   **/
  public Vector find_controlling_invariants()
  {
    // We used to assume there was at most one of these, but that
    // turned out to be wrong.  If this ppt has more equality
    // invariants than the controller, two different invariants can
    // match.  For example, a controller might say "a > 0", "b > 0" as
    // two different invariants, but if this ppt also has "a == b"
    // then both invariants should be returned.  This especailly
    // matters if, for example, "a > 0" was obvious (and thus wouldn't
    // suppress this invariant).
    Vector results = new Vector();

    // System.out.println("find_controlling_invariant: " + format());
    PptTopLevel pptt = (PptTopLevel) ppt.parent;

    // Try to match inv against all controlling invariants
    Iterator controllers = pptt.controlling_ppts.iterator();
    while (controllers.hasNext()) {
      PptTopLevel controller = (PptTopLevel) controllers.next();
      // System.out.println("Looking for controller of " + format() + " in " + controller.name);
      Iterator candidates = controller.invariants_iterator(); // unstable
      while (candidates.hasNext()) {
	Invariant cand_inv = (Invariant) candidates.next();
	if (isSameInvariant(cand_inv)) {
	  // System.out.println("Controller found: " + cand_inv.format() + " [worth printing: " + cand_inv.isWorthPrinting() + "]]");
	  results.add(cand_inv);
	}
        // System.out.println("Failed candidate: " + cand_inv.format());
      }
    }

    return results;
  }

  // For reproducible results when debugging
  static Comparator invComparator = new Invariant.ClassVarnameComparator();
  public Vector find_controlling_invariants_sorted() {
    Vector unsorted = find_controlling_invariants();
    Invariant[] invs = (Invariant[]) unsorted.toArray(new Invariant[0]);
    Arrays.sort(invs, invComparator);
    Vector result = new Vector(invs.length);
    for (int i=0; i<invs.length; i++)
      result.add(invs[i]);
    return result;
  }

  // Uninteresting invariants will override this method to return
  // false
  public boolean isInteresting() {
    return true;
  }

  // Orders invariants by class, then by variable names.  If the
  // invariants are both of class Implication, they are ordered by
  // comparing the predicate, then the consequent.
  public static final class ClassVarnameComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      Invariant inv1 = (Invariant) o1;
      Invariant inv2 = (Invariant) o2;

      if (inv1 instanceof Implication && inv2 instanceof Implication)
        return compareImplications((Implication) inv1, (Implication) inv2);

      int compareClass = compareClass(inv1, inv2);
      if (compareClass != 0)
        return compareClass;

      return compareVariables(inv1, inv2);
    }

    // Returns 0 if the invariants are of the same class.  Else,
    // returns the comparison of the class names.
    private int compareClass(Invariant inv1, Invariant inv2) {
      if (inv1.getClass().equals(inv2.getClass())) {
        return 0;
      } else {
        String classname1 = inv1.getClass().getName();
        String classname2 = inv2.getClass().getName();
        return classname1.compareTo(classname2);
      }
    }

    // Returns 0 if the invariants have the same variable names.
    // Else, returns the comparison of the first variable names that
    // differ.  Requires that the invariants be of the same class.
    private int compareVariables(Invariant inv1, Invariant inv2) {
      VarInfo[] vars1 = inv1.ppt.var_infos;
      VarInfo[] vars2 = inv2.ppt.var_infos;

      // due to inv type match already
      Assert.assert(vars1.length == vars2.length);

      for (int i=0; i < vars1.length; i++) {
        VarInfo var1 = vars1[i];
        VarInfo var2 = vars2[i];
        int compare = var1.name.compareTo(var2.name);
        if (compare != 0) return compare;
      }

      // All the variable names matched
      return 0;
    }

    private int compareImplications(Implication inv1, Implication inv2) {
      int comparePredicate = compare(inv1.predicate, inv2.predicate);
      if (comparePredicate != 0)
        return comparePredicate;

      return compare(inv1.consequent, inv2.consequent);
    }
  }

}


//     def format(self, args=None):
//         if self.one_of:
//             # If it can be None, print it only if it is always None and
//             # is an invariant over non-derived variable.
//             if self.can_be_None:
//                 if ((len(self.one_of) == 1)
//                     and self.var_infos):
//                     some_nonderived = false
//                     for vi in self.var_infos:
//                      some_nonderived = some_nonderived or not vi.is_derived
//                     if some_nonderived:
//                         return "%s = uninit" % (args,)
//             elif len(self.one_of) == 1:
//                 return "%s = %s" % (args, self.one_of[0])
//             ## Perhaps I should unconditionally return this value;
//             ## otherwise I end up printing ranges more often than small
//             ## numbers of values (because when few values and many samples,
//             ## the range always looks justified).
//             # If few samples, don't try to infer a function over the values;
//             # just return the list.
//             elif (len(self.one_of) <= 3) or (self.samples < 100):
//                 return "%s in %s" % (args, util.format_as_set(self.one_of))
//         return None
