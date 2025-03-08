package com.example.androidcicd;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.androidcicd.movie.Movie;
import com.example.androidcicd.movie.MovieProvider;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MovieProviderTest {
    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockMovieCollection;

    @Mock
    private DocumentReference mockDocRef;

    private MovieProvider movieProvider; // No need to mock this as it is what we are testing

    @Mock
    private MovieProvider.DataStatus mockDataStatus;
    @Before
    public void setUp() {
        System.out.println("Starting setup...");

        // Start up mocks
        MockitoAnnotations.openMocks(this);

        // Ensure Firestore mocks return valid references
        when(mockFirestore.collection("movies")).thenReturn(mockMovieCollection);
        when(mockMovieCollection.document()).thenReturn(mockDocRef);
        when(mockMovieCollection.document(anyString())).thenReturn(mockDocRef);

        // Ensure mockDocRef returns a valid ID
        when(mockDocRef.getId()).thenReturn("123");

        // Ensure mock DataStatus is initialized to prevent null exceptions
        mockDataStatus = mock(MovieProvider.DataStatus.class);

        // Ensure Firestore Task mock doesn't return null
        Task<QuerySnapshot> mockTask = mock(Task.class);
        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);
        when(mockTask.isSuccessful()).thenReturn(true);
        when(mockTask.getResult()).thenReturn(mockQuerySnapshot);
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);
        when(mockMovieCollection.whereEqualTo(anyString(), anyString()).get()).thenReturn(mockTask);

        // Setup the movie provider
        MovieProvider.setInstanceForTesting(mockFirestore);
        movieProvider = MovieProvider.getInstance(mockFirestore);

        System.out.println("MovieProvider initialized successfully.");
    }



    @Test
    public void testAddMovieSetsId() {
        // Movie to add
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);

        // Define the ID we want to set for the movie
        when(mockDocRef.getId()).thenReturn("123");

        // Simulate Firestore returning a successful task
        Task<Void> mockSetTask = mock(Task.class);
        when(mockDocRef.set(movie)).thenReturn(mockSetTask);
        when(mockSetTask.isSuccessful()).thenReturn(true);

        // Add movie
        movieProvider.addMovie(movie, mockDataStatus);

        // Verify movie ID is set correctly
        assertEquals("Movie was not updated with correct id.", "123", movie.getId());

        // Verify Firestore interaction
        verify(mockDocRef).set(movie);
    }

    @Test
    public void testDeleteMovie() {
        // Create movie and set our id
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);
        movie.setId("123");

        // Call deleteMovie and verify Firestore delete is called
        movieProvider.deleteMovie(movie);
        verify(mockDocRef).delete();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateMovieShouldThrowErrorForDifferentIds() {
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);
        movie.setId("1");

        // Make sure the doc ref has a different ID
        when(mockDocRef.getId()).thenReturn("123");

        // Call update movie
        movieProvider.updateMovie(movie, "Another Title", "Another Genre", 2026, mockDataStatus);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateMovieShouldThrowErrorForEmptyName() {
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);
        movie.setId("123");

        when(mockDocRef.getId()).thenReturn("123");

        // Call update movie
        movieProvider.updateMovie(movie, "", "Another Genre", 2026, mockDataStatus);
    }

    @Test
    public void testAddDuplicateMovieFails() {
        Movie movie = new Movie("Oppenheimer", "Thriller", 2023);

        Task<QuerySnapshot> mockTask = mock(Task.class);
        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);
        when(mockTask.isSuccessful()).thenReturn(true);
        when(mockTask.getResult()).thenReturn(mockQuerySnapshot);
        when(mockQuerySnapshot.isEmpty()).thenReturn(false); // Simulating a duplicate movie exists

        when(mockMovieCollection.whereEqualTo("title", "Oppenheimer").get()).thenReturn(mockTask);

        movieProvider.addMovie(movie, mockDataStatus);

        verify(mockDataStatus).onError("A movie with this title already exists.");
    }
}


