package pl.charmas.android.reactivelocation2

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.IntRange
import androidx.annotation.RequiresPermission
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import io.reactivex.Maybe
import pl.charmas.android.reactivelocation2.ext.toMaybe

class ReactivePlacesProvider constructor(
    val context: Context,
) {

    private val placesClient = Places.createClient(context)

    /**
     * Returns observable that fetches current place from Places API. To flatmap and auto release
     * buffer to {@link com.google.android.gms.location.places.PlaceLikelihood} observable use
     * {@link DataBufferObservable}.
     *
     * @param placeFilter filter
     * @return observable that emits current places buffer and completes
     */
    @RequiresPermission(allOf = ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_WIFI_STATE"])
    fun getCurrentPlace(placeFilter: FindCurrentPlaceRequest): Maybe<FindCurrentPlaceResponse> {
        return placesClient.findCurrentPlace(placeFilter)
            .toMaybe()
    }

    /**
     * Returns observable that fetches autocomplete predictions from Places API. To flatmap and autorelease
     * {@link com.google.android.gms.location.places.AutocompletePredictionBuffer} you can use
     * {@link DataBufferObservable}.
     *
     * @param query  search query
     * @param bounds bounds where to fetch suggestions from
     * @param filter filter
     * @return observable with suggestions buffer and completes
     *
     * @deprecated use {@link ReactiveLocationProvider#getPlaceCompatAutocompletePredictions(java.lang.String, com.google.android.gms.maps.model.LatLngBounds, com.google.android.libraries.places.compat.AutocompleteFilter)}
     */
    fun getPlaceAutocompletePredictions(result: FindAutocompletePredictionsRequest): Maybe<List<AutocompletePrediction>> {
        return placesClient
            .findAutocompletePredictions(result)
            .toMaybe()
            .map { obj: FindAutocompletePredictionsResponse -> obj.autocompletePredictions }
    }

    /**
     * Returns observable that fetches photo metadata from the Places API using the place ID.
     *
     * @param placeId id for place
     * @return observable that emits metadata buffer and completes
     */
    fun getPhotoMetadataById(
        placeId: String,
        @IntRange(from = 0L) height: Int,
        @IntRange(from = 0L) width: Int,
    ): Maybe<Bitmap> {
        return getPlaceById(placeId, listOf(Place.Field.PHOTO_METADATAS))
            .flatMap { res ->
                val photoMetadata = res.place.photoMetadatas?.firstOrNull()
                if (photoMetadata == null) {
                    Maybe.empty<Bitmap>()
                } else {
                    placesClient
                        .fetchPhoto(
                            FetchPhotoRequest.builder(
                                PhotoMetadata.builder(placeId)
                                    .setHeight(height)
                                    .setWidth(width)
                                    .setAttributions(photoMetadata.attributions)
                                    .build()
                            )
                                .build()
                        )
                        .toMaybe()
                        .map { it.bitmap }
                }
            }
    }

    fun getPlaceById(
        placeId: String,
        fields: List<Place.Field>,
    ): Maybe<FetchPlaceResponse> {
        return placesClient
            .fetchPlace(
                FetchPlaceRequest.builder(
                    placeId,
                    fields
                ).build()
            )
            .toMaybe()
    }

    /**
     * Returns observable that fetches a placePhotoMetadata from the Places API using the place placePhotoMetadata metadata.
     * Use after fetching the place placePhotoMetadata metadata with [ReactiveLocationProvider.getPhotoMetadataById]
     *
     * @param placePhotoMetadata the place photo meta data
     * @return observable that emits the photo result and completes
     */
    fun getPhotoForMetadata(placePhotoMetadata: PhotoMetadata): Maybe<Bitmap> {
        return placesClient
            .fetchPhoto(
                FetchPhotoRequest.builder(placePhotoMetadata)
                    .build()
            )
            .toMaybe()
            .map { it.bitmap }
    }

    /**
     * Returns observable that fetches a place from the Places API using the place ID.
     *
     * @param placeId id for place
     * @return observable that emits places buffer and completes
     *
     * @deprecated use {@link ReactiveLocationProvider#getPlaceCompatById(java.lang.String)}
     */
    fun getPlaceById(placeId: String): Maybe<FetchPlaceResponse> {
        val listOf = listOf(
            Place.Field.ID,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS
        )
        return getPlaceById(placeId, listOf)
    }
}
