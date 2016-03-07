package br.victor.osmdroidexemplo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.PathOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {
    private MapController mc;
    private LocationManager locationManager;
    MapView map;
    private PathOverlay po;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setMultiTouchControls(true);

        mc = (MapController) map.getController();
        mc.setZoom(16);

        GeoPoint center = new GeoPoint(-5.8419026, -35.1984748);
        mc.animateTo(center);

        initPathOverlay();
        addMarker(center);


        map.setMapListener(new MapListener() {

            @Override
            public boolean onScroll(ScrollEvent arg0) {
                Log.i("Script", "onScroll()");
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent arg0) {
                Log.i("Script", "onZoom()");
                return false;
            }

        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRoute();
            }
        });


    }

    public void addMarker(GeoPoint center) {
        Marker marker = new Marker(map);
        marker.setPosition(center);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.mipmap.ic_launcher));
        marker.setDraggable(true);


        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker arg0, MapView arg1) {
                Log.i("Script", "onMarkerClick()");
                return false;
            }

        });

        map.getOverlays().clear();
        map.getOverlays().add(addPointsLine(center));
        map.getOverlays().add(new MapOverlay(this));
        map.getOverlays().add(marker);
        map.invalidate();
    }

    // PATH OVERLAY
    public void initPathOverlay() {
        po = new PathOverlay(0, this);
        Paint p = new Paint();
        p.setColor(Color.RED);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);
        po.setPaint(p);
    }


    public PathOverlay addPointsLine(GeoPoint gp) {
        po.addPoint(gp);
        return (po);
    }

    class MapOverlay extends Overlay {

        public MapOverlay(Context ctx) {
            super(ctx);
        }

        @Override
        protected void draw(Canvas arg0, MapView arg1, boolean arg2) {
        }


        @Override
        public boolean onSingleTapConfirmed(MotionEvent me, MapView mv) {
            Projection p = map.getProjection();
            GeoPoint gp = (GeoPoint) p.fromPixels((int) me.getX(), (int) me.getY());
            addMarker(gp);
            return (false);
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        GeoPoint center = new GeoPoint(location.getLatitude(), location.getLongitude());
        mc.animateTo(center);
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    // ROUTE
    public void getRoute() {
        new Thread() {
            public void run() {
                GeoPoint start = getLocation("R. das Exatas, Natal - RN");
                GeoPoint end = getLocation("Rua da Saúde, Lagoa Nova, Natal");

                if (start != null && end != null) {
                    drawRoute(start, end);
                } else {
                    Toast.makeText(MainActivity.this, "FAIL!", Toast.LENGTH_SHORT).show();
                }
            }
        }.start();
    }


    public GeoPoint getLocation(String location) {
        GeocoderNominatim gn = new GeocoderNominatim(getApplicationContext(), "br.victor.osmdroidexemplo");
        GeoPoint gp = null;
        List<Address> al = new ArrayList<Address>();

        try {
            al = gn.getFromLocationName(location, 1);

            if (al != null && al.size() > 0) {
                Log.i("Script", "Rua: " + al.get(0).getThoroughfare());
                Log.i("Script", "Cidade: " + al.get(0).getSubAdminArea());
                Log.i("Script", "Estado: " + al.get(0).getAdminArea());
                Log.i("Script", "País: " + al.get(0).getCountryName());

                gp = new GeoPoint(al.get(0).getLatitude(), al.get(0).getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return (gp);
    }

    public void drawRoute(GeoPoint start, GeoPoint end) {
        RoadManager roadManager = new OSRMRoadManager(getApplicationContext());
        ArrayList<GeoPoint> points = new ArrayList<GeoPoint>();
        points.add(start);
        points.add(end);
        Road road = roadManager.getRoad(points);
        final Polyline roadOverlay = RoadManager.buildRoadOverlay(road, MainActivity.this);

        runOnUiThread(new Runnable() {
            public void run() {
                map.getOverlays().add(roadOverlay);
            }
        });
    }

}

