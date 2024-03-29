package com.matt.forgehax.asm.coremod.utils.asmtype.builders;

import com.matt.forgehax.asm.ASMCommon;
import com.matt.forgehax.asm.coremod.utils.asmtype.ASMClass;
import com.matt.forgehax.asm.coremod.utils.asmtype.ASMMethod;
import com.matt.forgehax.asm.utils.name.IName;
import com.matt.forgehax.asm.utils.name.NameBuilder;
import java.util.Objects;

import org.objectweb.asm.Type;

/** Created on 5/27/2017 by fr1kin */
public class ASMMethodBuilder implements ASMCommon {
  private static final IName<Type>[] NO_PARAMETERS = new ParameterBuilder().asArray();

  private ASMClass parentClass = null;
  private String name = null, srgName = null;
  private IName<Type>[] parameterTypes = null;
  private IName<Type> returnType = null;

  private boolean auto = false;

  protected ASMMethodBuilder() {}

  public ASMMethodBuilder setParentClass(ASMClass parentClass) {
    this.parentClass = parentClass;
    return this;
  }

  public ASMMethodBuilder setParentClass(Type type) {
    return setParentClass(ASMBuilders.newClassBuilder().setClassName(type).build());
  }

  public ASMMethodBuilder setParentClass(String internalClassName) {
    return setParentClass(ASMBuilders.newClassBuilder().setClassName(internalClassName).build());
  }

  public ASMMethodBuilder setParentClass(Class<?> clazz) {
    return setParentClass(ASMBuilders.newClassBuilder().setClassName(clazz).build());
  }

  public ASMMethodBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public ASMMethodBuilder setSrgName(String srgName) {
    this.srgName = srgName;
    return this;
  }

  public ASMMethodBuilder setParameterTypes(IName<Type>[] parameterTypes) {
    this.parameterTypes = parameterTypes;
    return this;
  }

  public ASMMethodBuilder emptyParameters() {
    return setParameterTypes(NO_PARAMETERS);
  }

  public ParameterBuilder beginParameters() {
    return new ParameterBuilder(this);
  }

  public ASMMethodBuilder setReturnType(IName<Type> returnType) {
    this.returnType = returnType;
    return this;
  }

  public ASMMethodBuilder setReturnType(Type returnType) {
    return setReturnType(NameBuilder.createSingleName(returnType));
  }

  public ASMMethodBuilder setReturnType(String internalClassName) {
    return setReturnType(Type.getObjectType(internalClassName));
  }

  public ASMMethodBuilder setReturnType(Class<?> clazz) {
    return setReturnType(Type.getType(clazz));
  }

  public ASMMethodBuilder setReturnType(ASMClass clazz) {
    return setReturnType(clazz.getAll());
  }

  // TODO: implement
  @Deprecated // might be implemented later
  public ASMMethodBuilder autoAssign() {
    throw new UnsupportedOperationException("autoAssign for method");
    //auto = true;
    //return this;
  }

  private void attemptAutoAssign() {
    // build parameter list for normal state
    Type[] normalParameters = new Type[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++) normalParameters[i] = parameterTypes[i].get();
    // create method descriptor
    String descriptor = Type.getMethodType(returnType.get(), normalParameters).getDescriptor();

    setSrgName(ASMCommon.MAPPER.getSrgMethodName(parentClass.getInternalName(), name, descriptor));
  }

  public ASMMethod build() {
    // parent class not required
    Objects.requireNonNull(name, "Missing method name");
    Objects.requireNonNull(
        parameterTypes, "Missing method parameters (use emptyParameters() if none are present)");
    Objects.requireNonNull(returnType, "Missing method return type");
    if (auto) attemptAutoAssign();
    return new ASMMethod(
        parentClass, NameBuilder.create(name, srgName), parameterTypes, returnType);
  }
}
