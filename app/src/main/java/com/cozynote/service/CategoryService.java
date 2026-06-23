package com.cozynote.service;

import java.io.IOException;
import java.util.List;

import com.cozynote.domain.Category;
import com.cozynote.persistence.SqliteNoteRepository;

public final class CategoryService {
    private static final String DEFAULT_CATEGORY = "모든 메모";

    private final SqliteNoteRepository repository;

    public CategoryService(SqliteNoteRepository repository) {
        this.repository = repository;
    }

    public List<Category> listCategories() throws IOException {
        return repository.findCategories();
    }

    public Category createCategory(String name, int level, int sortOrder) throws IOException {
        Category category = new Category(name, 0, level);
        repository.saveCategory(category, sortOrder);
        return category;
    }

    public void deleteCategory(Category category) throws IOException {
        if (category == null) {
            return;
        }
        if (DEFAULT_CATEGORY.equals(category.name())) {
            throw new IllegalArgumentException("기본 카테고리는 삭제할 수 없습니다.");
        }
        repository.deleteCategory(category.name());
    }

    public boolean isDefaultCategory(Category category) {
        return category != null && DEFAULT_CATEGORY.equals(category.name());
    }
}
