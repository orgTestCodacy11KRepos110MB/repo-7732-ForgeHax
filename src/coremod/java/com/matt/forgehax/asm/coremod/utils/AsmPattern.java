package com.matt.forgehax.asm.coremod.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.matt.forgehax.asm.coremod.utils.asmtype.ASMField;
import com.matt.forgehax.asm.coremod.utils.asmtype.ASMMethod;
import org.objectweb.asm.tree.*;

public class AsmPattern {

  public static final int IGNORE_FRAMES = 1 << 0;
  public static final int IGNORE_LABELS = 1 << 1;
  public static final int IGNORE_LINENUMBERS = 1 << 2;
  public static final int CODE_ONLY = IGNORE_FRAMES | IGNORE_LABELS | IGNORE_LINENUMBERS;

  private final int flags;
  private final List<Predicate<AbstractInsnNode>> insnPredicates;

  private AsmPattern(List<Predicate<AbstractInsnNode>> predicates, int flags) {
    this.insnPredicates = predicates;
    this.flags = flags;
  }

  public InsnPattern test(MethodNode main) {
    return test(main.instructions.getFirst());
  }

  public InsnPattern test(AbstractInsnNode start) {
    return ASMHelper.findPattern(
        start,
        insnPredicates.size(),
        // isValidNode
        (node) ->
            !testFlag(node, FrameNode.class, IGNORE_FRAMES)
                && !testFlag(node, LabelNode.class, IGNORE_LABELS)
                && !testFlag(node, LineNumberNode.class, IGNORE_LINENUMBERS),
        // nodePredicate
        (found, node) -> insnPredicates.get(found).test(node),
        InsnPattern::new);
  }

  // returns true if the node is an instance of the given type and the given flag is present
  private boolean testFlag(
      AbstractInsnNode node, Class<? extends AbstractInsnNode> type, int flag) {
    return type.isInstance(node) && (this.flags & flag) != 0;
  }

  public static class Builder {

    private final int flags;
    private final List<Predicate<AbstractInsnNode>> predicates = new ArrayList<>();

    public Builder(final int flags) {
      this.flags = flags;
    }

    public Builder opcode(int opcode) {
      return add(insn -> insn.getOpcode() == opcode);
    }

    public Builder opcodes(int... opcodes) {
      for (int o : opcodes) {
        opcode(o);
      }
      return this;
    }

    public Builder invoke() {
      return add(insn -> insn instanceof MethodInsnNode);
    }

    public Builder any() {
      return add(insn -> true);
    }

    public Builder label() {
      if ((flags & IGNORE_LABELS) != 0)
        throw new IllegalStateException("Attempting to find a label with flag IGNORE_LABELS");
      return add(insn -> insn instanceof LabelNode);
    }

    public Builder ASMType(int opcode, ASMField field) {
      return this.custom(insn -> insn.getOpcode() == opcode &&
              ((FieldInsnNode)insn).name.equals(field.getRuntimeName()) &&
              ((FieldInsnNode)insn).desc.equals(field.getRuntimeDescriptor())
        );
    }

    public Builder ASMType(int opcode, ASMMethod method) {
      return this.custom(insn -> insn.getOpcode() == opcode &&
            ((MethodInsnNode)insn).name.equals(method.getRuntimeName()) &&
            ((MethodInsnNode)insn).desc.equals(method.getRuntimeDescriptor())
        );
    }

    public <T extends AbstractInsnNode> Builder custom(Predicate<T> predicate) {
      return add(predicate);
    }

    private Builder add(Predicate<? extends AbstractInsnNode> predicate) {
      predicates.add((Predicate<AbstractInsnNode>) predicate);
      return this;
    }

    public AsmPattern build() {
      return new AsmPattern(predicates, flags);
    }
  }
}
