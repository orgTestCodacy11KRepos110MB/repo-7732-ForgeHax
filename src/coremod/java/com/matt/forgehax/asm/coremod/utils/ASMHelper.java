package com.matt.forgehax.asm.coremod.utils;

import static org.objectweb.asm.Opcodes.*;

import com.matt.forgehax.asm.coremod.utils.asmtype.ASMClass;
import com.matt.forgehax.asm.coremod.utils.asmtype.ASMField;
import com.matt.forgehax.asm.coremod.utils.asmtype.ASMMethod;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import cpw.mods.modlauncher.api.ITransformer;
import org.objectweb.asm.tree.*;

public class ASMHelper {

  public static Set<ITransformer.Target> getTargetSet(ASMClass... classes) {
    return getTargetSet(ASMHelper::getTarget, classes);
  }

  public static Set<ITransformer.Target> getTargetSet(ASMMethod... methods) {
    return getTargetSet(ASMHelper::getTarget, methods);
  }

  public static Set<ITransformer.Target> getTargetSet(ASMField... fields) {
    return getTargetSet(ASMHelper::getTarget, fields);
  }

  public static <T> Set<ITransformer.Target> getTargetSet(Function<T, ITransformer.Target>  toTarget, T... members) {
    return Stream.of(members)
            .map(toTarget)
            .collect(Collectors.toSet());
  }

  public static ITransformer.Target getTarget(ASMClass clazz) {
    return ITransformer.Target.targetClass(clazz.getRuntimeInternalName());
  }
  public static ITransformer.Target getTarget(ASMMethod method) {
    return ITransformer.Target.targetMethod(
        method.getParentClass().getInternalName(), // TODO: parent class should be guaranteed to exist
        method.getRuntimeName(),
        method.getRuntimeDescriptor()
    );
  }
  public static ITransformer.Target getTarget(ASMField field) {
    return ITransformer.Target.targetField(
        field.getParentClass().getInternalName(), // TODO: parent class should be guaranteed to exist
        field.getRuntimeName()
    );
  }


  /**
   * Finds a pattern of opcodes and returns the first node of the matched pattern if found
   *
   * @param start starting node
   * @param pattern integer array of opcodes
   * @param mask same length as the pattern. 'x' indicates the node will be checked, '?' indicates
   *     the node will be skipped over (has a bad opcode)
   * @return top node of matching pattern or null if nothing is found
   */
  public static AbstractInsnNode findPattern(AbstractInsnNode start, int[] pattern, char[] mask) {
    if (pattern.length != mask.length)
      throw new IllegalArgumentException("Mask must be same length as pattern");
    return findPattern(
        start,
        pattern.length,
        (node) -> true,
        (found, next) -> mask[found] != 'x' || next.getOpcode() == pattern[found],
        (first, last) -> first);
  }

  // TODO: This really needs to be rewritten
  public static <T> T findPattern(
      final AbstractInsnNode start,
      final int patternSize,
      Predicate<AbstractInsnNode>
          isValidNode, // if this returns false then dont invoke the predicate and dont update found
      BiPredicate<Integer, AbstractInsnNode> nodePredicate,
      BiFunction<AbstractInsnNode, AbstractInsnNode, T> outputFunction) {
    if (start != null) {
      int found = 0;
      AbstractInsnNode next = start;
      do {
        // Check if node matches the predicate.
        // If the node is not considered a "valid" node then we'll consider it to match the pattern
        // but the predicate will not be invoked for it and found will not be incremented
        final boolean validNode = isValidNode.test(next);
        if (!validNode || nodePredicate.test(found, next)) {
          if (validNode) {
            // Increment number of matched opcodes
            found++;
          }
        } else {
          // Go back to the starting node
          for (int i = 1; i <= (found - 1); i++) {
            next = next.getPrevious();
          }
          // Reset the number of insns matched
          found = 0;
        }

        // Check if found entire pattern
        if (found >= patternSize) {
          final AbstractInsnNode end = next;
          // Go back to top node
          for (int i = 1; i <= (found - 1); i++) {
            do {
                next = next.getPrevious();
            } while (!isValidNode.test(next));
          }
          return outputFunction.apply(next, end);
        }
        next = next.getNext();
      } while (next != null);
    }
    // failed to find pattern
    return null;
  }

  public static AbstractInsnNode findPattern(AbstractInsnNode start, int[] pattern, String mask) {
    return findPattern(start, pattern, mask.toCharArray());
  }

  public static AbstractInsnNode findPattern(AbstractInsnNode start, int... opcodes) {
    StringBuilder mask = new StringBuilder();
    for (int op : opcodes) mask.append(op == MagicOpcodes.NONE ? '?' : 'x');
    return findPattern(start, opcodes, mask.toString());
  }

  public static AbstractInsnNode findPattern(InsnList instructions, int... opcodes) {
    return findPattern(instructions.getFirst(), opcodes);
  }

  public static AbstractInsnNode findPattern(MethodNode node, int... opcodes) {
    return findPattern(node.instructions, opcodes);
  }

  @Nullable
  public static AbstractInsnNode forward(AbstractInsnNode start, int n) {
    AbstractInsnNode node = start;
    for (int i = 0;
        i < Math.abs(n) && node != null;
        ++i, node = n > 0 ? node.getNext() : node.getPrevious()) ;
    return node;
  }

  public static String getClassData(ClassNode node) {
    StringBuilder builder = new StringBuilder("METHODS:\n");
    for (MethodNode method : node.methods) {
      builder.append("\t");
      builder.append(method.name);
      builder.append(method.desc);
      builder.append("\n");
    }
    builder.append("\nFIELDS:\n");
    for (FieldNode field : node.fields) {
      builder.append("\t");
      builder.append(field.desc);
      builder.append(" ");
      builder.append(field.name);
      builder.append("\n");
    }
    return builder.toString();
  }

  public static MethodInsnNode call(int opcode, boolean isInterface, ASMMethod method) {
    Objects.requireNonNull(method.getParentClass(), "Method requires assigned parent class");
    return new MethodInsnNode(
        opcode,
        method.getParentClass().getRuntimeInternalName(),
        method.getRuntimeName(),
        method.getRuntimeDescriptor(),
        false);
  }

  public static MethodInsnNode call(int opcode, ASMMethod method) {
    return call(opcode, false, method);
  }

  public static FieldInsnNode call(int opcode, ASMField field) {
    Objects.requireNonNull(field.getParentClass(), "Field requires assigned parent class");
    return new FieldInsnNode(
        opcode,
        field.getParentClass().getRuntimeInternalName(),
        field.getRuntimeName(),
        field.getRuntimeDescriptor());
  }

  // scope is from first label to last label
  public static int addNewLocalVariable(MethodNode method, String name, String desc) {
    AsmPattern labelPattern = new AsmPattern.Builder(0).label().build();

    final LabelNode start = labelPattern.test(method).getFirst();

    // TODO: implement backwards pattern matching so this can be refactored
    AbstractInsnNode iter = method.instructions.getFirst();
    LabelNode end = null;
    do {
      if (iter instanceof LabelNode) end = (LabelNode) iter;
      iter = iter.getNext();
    } while (iter != null);
    if (end == null) throw new IllegalArgumentException("Failed to find LabelNode");

    return addNewLocalVariable(method, name, desc, start, end);
  }

  public static int addNewLocalVariable(
      MethodNode method, String name, String desc, LabelNode start, LabelNode end) {
    Optional<LocalVariableNode> lastVar =
        method.localVariables.stream().max(Comparator.comparingInt(var -> var.index));
    final int newIndex =
        lastVar.map(var -> var.desc.matches("[JD]") ? var.index + 2 : var.index + 1).orElse(0);

    LocalVariableNode variable = new LocalVariableNode(name, desc, null, start, end, newIndex);
    method.localVariables.add(variable);

    return newIndex;
  }

  public static InsnList newInstance(String name, ASMClass[] argTypes, @Nullable InsnList args) {
    final String desc = Stream.of(argTypes).map(ASMClass::getName).collect(Collectors.joining("", "(", ")V"));
    return newInstance(name, desc, args);
  }

  // args should be type descriptors
  public static InsnList newInstance(String name, String[] argTypes, @Nullable InsnList args) {
    final String desc = Stream.of(argTypes).collect(Collectors.joining("", "(", ")V"));
    return newInstance(name, desc, args);
  }

  public static InsnList newInstance(String name, String desc, @Nullable InsnList args) {
    InsnList list = new InsnList();
    list.add(new TypeInsnNode(NEW, name));
    list.add(new InsnNode(DUP));
    if (args != null) list.add(args);
    list.add(new MethodInsnNode(INVOKESPECIAL, name, "<init>", desc, false));
    return list;
  }

  public interface MagicOpcodes {
    int NONE = -666;
  }
}
