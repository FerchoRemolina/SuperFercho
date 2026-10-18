/**
 * Módulo media: almacenamiento de imágenes administradas por SuperFercho
 * (PNG, JPEG y WebP subidas por el Admin). Los archivos se nombran con UUID
 * server-side y se sirven públicamente bajo /api/v1/media/images. Los demás
 * módulos (Storefront, futuros banners y bloques publicitarios) guardan
 * únicamente la URL devuelta y nunca acceden al almacenamiento directamente.
 */
package com.superfercho.media;
