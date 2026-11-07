package com.clientfiles.ui.kit;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

/** Base of custom-painted components: a plain JComponent exposes nothing to screen readers. */
public abstract class Widget extends JComponent {
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return isFocusable() ? AccessibleRole.PUSH_BUTTON : AccessibleRole.LABEL;
                }
            };
        }
        return accessibleContext;
    }
}
