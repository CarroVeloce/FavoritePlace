package pnj.uts.ti.favoritplace;

public class FavoritePlace {
    private String name;
    private double latitude;
    private double longitude;

    public FavoritePlace() {
        // Default constructor required for calls to DataSnapshot.getValue(FavoritePlace.class)
    }

    public FavoritePlace(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public FavoritePlace(String name, double latitude, double longitude, String address) {
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
