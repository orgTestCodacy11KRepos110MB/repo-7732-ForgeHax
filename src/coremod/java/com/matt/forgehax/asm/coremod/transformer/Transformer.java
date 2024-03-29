package com.matt.forgehax.asm.coremod.transformer;

import com.matt.forgehax.asm.ASMCommon;
import com.matt.forgehax.asm.coremod.TypesMc;
import cpw.mods.modlauncher.TransformTargetLabel;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nonnull;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.Stream;

public interface Transformer<T> extends ITransformer<T>, ASMCommon, TypesMc, Opcodes {

    @Nonnull
    @Override
    default TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @SuppressWarnings("unchecked")
    static <T> ITransformer<T> createWrapper(ITransformer<T> transformer, RegisterTransformer annotation) {
        Optional<ParameterizedType> parameterizedType =  Stream.of(transformer.getClass().getGenericInterfaces())
                .filter(type -> type instanceof ParameterizedType)
                .map(type -> (ParameterizedType)type)
                .filter(pType -> pType.getRawType().equals(Transformer.class) || pType.getRawType().equals(ITransformer.class))
                .findFirst();

        Type nodeType = parameterizedType
                .map(pType -> pType.getActualTypeArguments()[0])
                .orElseGet(annotation::value);

        TransformTargetLabel.LabelType labelType = TransformTargetLabel.LabelType.getTypeFor(nodeType)
            .orElseThrow(() -> new IllegalStateException("Class " + transformer.getClass() + " attempted to implement transformer for invalid node type"));
        switch (labelType) {
            case FIELD: return (ITransformer<T>)new FieldTransformerWrapper((ITransformer<FieldNode>)transformer);
            case METHOD: return (ITransformer<T>)new MethodTransformerWrapper((ITransformer<MethodNode>)transformer);
            case CLASS: return (ITransformer<T>)new ClassTransformerWrapper((ITransformer<ClassNode>)transformer);

            default: throw new IllegalStateException("??? " + transformer.getClass());
        }

    }

}
