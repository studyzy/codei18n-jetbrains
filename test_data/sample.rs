// Calculate fibonacci numbers
fn fib(n: u32) -> u32 {
    /// Returns the nth fibonacci number
    //! This is an inner doc comment
    /* Block comment for demonstration */
    if n <= 1 { 
        n 
    } else { 
        fib(n-1) + fib(n-2) 
    }
}

// Main entry point
fn main() {
    /** Outer doc comment
     * with multiple lines
     */
    println!("Fibonacci of 10 is {}", fib(10));
}
