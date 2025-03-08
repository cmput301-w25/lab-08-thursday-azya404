package com.example.androidcicd.movie;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MovieProvider {
    private static MovieProvider movieProvider;
    private final ArrayList<Movie> movies;
    private final CollectionReference movieCollection;

    private MovieProvider(FirebaseFirestore firestore) {
        movies = new ArrayList<>();
        movieCollection = firestore.collection("movies");
    }

    public interface DataStatus {
        void onDataUpdated();
        void onError(String error);
    }

    public void listenForUpdates(final DataStatus dataStatus) {
        movieCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                dataStatus.onError(error.getMessage());
                return;
            }
            movies.clear();
            if (snapshot != null) {
                for (QueryDocumentSnapshot item : snapshot) {
                    movies.add(item.toObject(Movie.class));
                }
                dataStatus.onDataUpdated();
            }
        });
    }

    public static MovieProvider getInstance(FirebaseFirestore firestore) {
        if (movieProvider == null)
            movieProvider = new MovieProvider(firestore);
        return movieProvider;
    }

    public ArrayList<Movie> getMovies() {
        return movies;
    }

    public void addMovie(Movie movie, DataStatus dataStatus) {
        Query query = movieCollection.whereEqualTo("title", movie.getTitle());
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                dataStatus.onError("A movie with this title already exists.");
                return;
            }

            DocumentReference docRef = movieCollection.document();
            movie.setId(docRef.getId());

            if (validMovie(movie)) {
                docRef.set(movie).addOnCompleteListener(addTask -> {
                    if (addTask.isSuccessful()) {
                        dataStatus.onDataUpdated();
                    } else {
                        dataStatus.onError("Failed to add movie.");
                    }
                });
            } else {
                dataStatus.onError("Invalid Movie!");
            }
        });
    }

    public void updateMovie(Movie movie, String title, String genre, int year, DataStatus dataStatus) {
        movieCollection.whereEqualTo("title", title).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    if (!document.getId().equals(movie.getId())) {
                        dataStatus.onError("A movie with this title already exists.");
                        return;
                    }
                }
            }

            movie.setTitle(title);
            movie.setGenre(genre);
            movie.setYear(year);
            DocumentReference docRef = movieCollection.document(movie.getId());

            if (validMovie(movie)) {
                docRef.set(movie).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        dataStatus.onDataUpdated();
                    } else {
                        dataStatus.onError("Failed to update movie.");
                    }
                });
            } else {
                dataStatus.onError("Invalid Movie!");
            }
        });
    }

    public void deleteMovie(Movie movie) {
        DocumentReference docRef = movieCollection.document(movie.getId());
        docRef.delete();
    }

    private boolean validMovie(Movie movie) {
        return !movie.getTitle().isEmpty() && !movie.getGenre().isEmpty() && movie.getYear() > 0;
    }

    public static void setInstanceForTesting(FirebaseFirestore firestore) {
        movieProvider = new MovieProvider(firestore);
    }
}



