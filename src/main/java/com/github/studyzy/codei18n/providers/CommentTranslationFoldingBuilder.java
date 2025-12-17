package com.github.studyzy.codei18n.providers;

import com.github.studyzy.codei18n.models.TranslatedComment;
import com.github.studyzy.codei18n.services.TranslationService;
import com.github.studyzy.codei18n.settings.DisplayMode;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注释翻译折叠构建器
 * 将英文注释折叠显示为中文翻译，用户可以展开查看英文原文
 */
public class CommentTranslationFoldingBuilder extends FoldingBuilderEx {
    
    private static final Logger LOG = Logger.getInstance(CommentTranslationFoldingBuilder.class);
    private static final FoldingGroup TRANSLATION_GROUP = FoldingGroup.newGroup("comment-translation");
    
    // 缓存翻译结果，用于getPlaceholderText方法
    private static final ConcurrentHashMap<Integer, String> translationCache = new ConcurrentHashMap<>();
    
    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(
            @NotNull PsiElement root,
            @NotNull Document document,
            boolean quick
    ) {
        PluginSettings settings = PluginSettings.getInstance();
        
        // 如果设置未初始化，返回空数组（用于测试环境）
        if (settings == null) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        
        // 如果插件未启用，返回空数组
        if (!settings.enabled) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        
        // 如果不是 FOLDING 模式，返回空数组
        if (settings.displayMode != DisplayMode.FOLDING) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        
        // 仅处理支持的文件类型（Go 和 Rust）
        if (!isSupportedFile(root)) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        
        // quick 模式下跳过，等待完整模式执行
        // 这是为了避免在快速扫描时阻塞 UI
        if (quick) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        
        try {
            String filename = root.getContainingFile().getName();
            LOG.info("Processing file: " + filename);
            
            // 使用同步方法获取翻译数据
            TranslationService translationService = TranslationService.getInstance(root.getProject());
            List<TranslatedComment> translations = translationService.getTranslationsSync(
                root.getContainingFile(), 
                5000  // 5秒超时
            );
            
            if (translations.isEmpty()) {
                LOG.info("No translations found for file: " + root.getContainingFile().getName());
                return FoldingDescriptor.EMPTY_ARRAY;
            }
            
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
                        if (translatedComment.startOffset() == startOffset) {
                            String translation = translatedComment.translation();
                            
                            if (translation != null && !translation.isEmpty()) {
                                // 格式化翻译文本（保留注释符号）
                                String placeholderText = formatTranslation(comment.getText(), translation);
                                
                                // 缓存翻译结果
                                translationCache.put(startOffset, placeholderText);
                                
                                // 创建折叠描述符
                                FoldingDescriptor descriptor = new FoldingDescriptor(
                                    comment.getNode(),
                                    comment.getTextRange(),
                                    TRANSLATION_GROUP,
                                    placeholderText
                                );
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
        // 从缓存中获取翻译文本
        int startOffset = node.getStartOffset();
        String cached = translationCache.get(startOffset);
        if (cached != null) {
            return cached;
        }
        // 如果没有缓存，返回默认文本
        return "...";
    }
    
    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        // 默认折叠（显示中文翻译）
        return true;
    }
    
    /**
     * 格式化翻译文本，保留注释符号
     * 如果翻译文本已经包含注释符号，则不再重复添加
     */
    private String formatTranslation(String originalComment, String translation) {
        // 去除翻译文本首尾空白
        String trimmedTranslation = translation.trim();
        
        if (originalComment.startsWith("///")) {
            if (trimmedTranslation.startsWith("///")) {
                return trimmedTranslation;
            }
            return "/// " + trimmedTranslation;
        } else if (originalComment.startsWith("//!")) {
            if (trimmedTranslation.startsWith("//!")) {
                return trimmedTranslation;
            }
            return "//! " + trimmedTranslation;
        } else if (originalComment.startsWith("//")) {
            // 检查翻译是否已经以 // 开头
            if (trimmedTranslation.startsWith("//")) {
                return trimmedTranslation;
            }
            return "// " + trimmedTranslation;
        } else if (originalComment.startsWith("/*") && originalComment.endsWith("*/")) {
            // 检查翻译是否已经是块注释格式
            if (trimmedTranslation.startsWith("/*") && trimmedTranslation.endsWith("*/")) {
                return trimmedTranslation;
            }
            return "/* " + trimmedTranslation + " */";
        }
        return trimmedTranslation;
    }
    
    /**
     * 检查是否为支持的文件类型
     * 当前支持：Go (.go), Rust (.rs)
     */
    private boolean isSupportedFile(PsiElement root) {
        String filename = root.getContainingFile().getName();
        if (filename == null) {
            return false;
        }
        return filename.endsWith(".go") || filename.endsWith(".rs");
    }
}