package com.superfercho.catalog.domain.model;

/**
 * Physical family used only for catalog presentation ordering. Declared order is the
 * deterministic cross-family sequence: MASS, then VOLUME, then DISCRETE.
 */
public enum PresentationDimension {
    MASS,
    VOLUME,
    DISCRETE
}
