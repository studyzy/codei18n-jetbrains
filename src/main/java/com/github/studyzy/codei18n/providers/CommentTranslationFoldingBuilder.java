package com.github.studyzy.codei18n.providers;

import com.github.studyzy.codei18n.models.TranslatedComment;
import com.github.studyzy.codei18n.services.TranslationService;
import com.github.studyzy.codei18n.settings.PluginSettings;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 注释翻译折叠构建器
 * 将英文注释折叠显示为中文翻译，用户可以展开查看英文原文
 */
public class CommentTranslationFoldingBuilder extends FoldingBuilderEx {
    
    private static final Logger LOG = Logger.getInstance(CommentTranslationFoldingBuilder.class);
    private static final FoldingGroup TRANSLATION_GROUP = FoldingGroup.newGroup("comment-translation");
    
    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(
            @NotNull PsiElement root,
            @NotNull Document document,
            boolean quick
    ) {
        // 如果插件未启用，返回空数组
        if (!PluginSettings.getInstance().enabled) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        
        // 仅处理 Go 文件
        if (!isGoFile(root)) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        
        // 如果是快速模式，返回空数组避免阻塞
        if (quick) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        
        try {
            // 获取翻译数据
            TranslationService translationService = TranslationService.getInstance(root.getProject());
            List<TranslatedComment> translations = translationService.getTranslations(
                root.getContainingFile(), 
                false
            );
            
            LOG.info("Building fold regions for " + root.getContainingFile().getName() + 
                    ", got " + translations.size() + " translations");
            
            // 遍历所有注释
            root.accept(new PsiRecursiveElementVisitor() {
                @Override
                public void visitComment(@NotNull PsiComment comment) {
                    super.visitComment(comment);
                    
                    int startOffset = comment.getTextRange().getStartOffset();
                    
                    // 查找匹配的翻译
                    for (TranslatedComment translatedComment : translations) {
                        if (translatedComment.getStartOffset() == startOffset) {
                            String translation = translatedComment.getTranslation();
                            
                            if (translation != null && !translation.isEmpty()) {
                                // 格式化翻译文本（保留注释符号）
                                String placeholderText = formatTranslation(comment.getText(), translation);
                                
                                // 创建折叠描述符，使用带占位符文本的构造函数
                                FoldingDescriptor descriptor = new FoldingDescriptor(
                                    comment.getNode(),
                                    comment.getTextRange(),
                                    TRANSLATION_GROUP
                                ) {
                                    @NotNull
                                    @Override
                                    public String getPlaceholderText() {
                                        return placeholderText;
                                    }
                                };
                                descriptors.add(descriptor);
                                
                                LOG.info("Added fold region for comment at offset " + startOffset + 
                                        ": " + placeholderText);
                            }
                            break;
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            LOG.error("Failed to build fold regions", e);
        }
        
        return descriptors.toArray(new FoldingDescriptor[0]);
    }
    
    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node) {
        // 占位符文本已在 FoldingDescriptor 构造时设置
        return null;
    }
    
    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        // 默认折叠（显示中文翻译）
        return true;
    }
    
    /**
     * 格式化翻译文本，保留注释符号
     */
    private String formatTranslation(String originalComment, String translation) {
        if (originalComment.startsWith("//")) {
            return "// " + translation;
        } else if (originalComment.startsWith("/*") && originalComment.endsWith("*/")) {
            return "/* " + translation + " */";
        }
        return translation;
    }
    
    /**
     * 检查是否为 Go 文件
     */
    private boolean isGoFile(PsiElement root) {
        return root.getContainingFile().getName().endsWith(".go");
    }
}
