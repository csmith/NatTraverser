/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.md87.nat;

/**
 *
 * @author chris
 */
public enum NatType {

    OPEN(true),
    FULL_CONE(true),
    RESTRICTED_CONE(false),
    PORT_RESTRICTED_CONE(false),
    SYMMETRIC(false);

    private final boolean canTraverseWithSymmetric;

    NatType(final boolean canTraverseWithSymmetric) {
        this.canTraverseWithSymmetric = canTraverseWithSymmetric;
    }

    public boolean canTraverseWith(final NatType type) {
        if (type == SYMMETRIC) {
            return canTraverseWithSymmetric;
        } else if (this == SYMMETRIC) {
            return type.canTraverseWith(this);
        } else  {
            return true;
        }
    }

}
