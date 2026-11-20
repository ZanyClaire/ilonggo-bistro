package com.ilonggobistro.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MenuCategoryForm {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must be 100 characters or fewer")
    private String name;

    @NotBlank(message = "Daypart label is required (e.g., All Day, Breakfast, Desserts)")
    @Size(max = 60, message = "Daypart must be 60 characters or fewer")
    private String daypart = "All Day";

    @Size(max = 255, message = "Description must be 255 characters or fewer")
    private String description;

    private int displayOrder = 10;

    public MenuCategoryForm() {
    }

    public static MenuCategoryForm fromEntity(MenuCategory category) {
        MenuCategoryForm form = new MenuCategoryForm();
        form.setName(category.getName());
        form.setDaypart(category.getDaypart());
        form.setDescription(category.getDescription());
        form.setDisplayOrder(category.getDisplayOrder());
        return form;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDaypart() {
        return daypart;
    }

    public void setDaypart(String daypart) {
        this.daypart = daypart;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
