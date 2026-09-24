package com.superfercho.shopping.application.port.out;

import com.superfercho.shopping.application.dto.favorite.FavoriteProductView;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductCardCatalogPort {

    List<FavoriteProductView> findCardsByIds(Collection<UUID> productIds);
}
