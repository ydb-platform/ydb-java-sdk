package tech.ydb.coordination.recipes.election;

/**
 * A listener interface for receiving leadership election events in a distributed system.
 *
 * <p>Implementations of this interface are notified when the current process becomes
 * the leader in a leader election scenario.</p>
 *
 * <h3>Leadership Lifecycle:</h3>
 * <ol>
 *   <li><b>Election:</b> The distributed system selects a leader</li>
 *   <li><b>Takeover:</b> {@code takeLeadership()} is invoked on the elected leader</li>
 *   <li><b>Execution:</b> The leader performs its duties while maintaining leadership</li>
 *   <li><b>Termination:</b> When {@code takeLeadership()} completes (either normally or exceptionally),
 *       the leadership is automatically relinquished and new elections begin</li>
 * </ol>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * LeaderElectionListener listener = new LeaderElectionListener() {
 *     public void takeLeadership() throws Exception {
 *         startServices();
 *
 *         // Main leadership work
 *         while (shouldContinueLeadership()) {
 *             performLeaderDuties();
 *         }
 *
 *         // Cleanup will trigger automatically when method exits
 *     }
 * };
 * }</pre>
 *
 * <p><b>Important Implementation Notes:</b></p>
 * <ul>
 *   <li>The leadership is maintained only while {@code takeLeadership()} is executing</li>
 *   <li>When the method completes (either normally or by throwing an exception), the leadership
 *       is automatically released and new elections begin immediately</li>
 *   <li>For long-running leadership, the method should not return until leadership should end</li>
 *   <li>To voluntarily relinquish leadership before completing, throw an exception</li>
 * </ul>
 *
 * <p><b>Error Handling:</b> If the implementation throws an exception, the leadership will be
 * released and new elections will be triggered, just as with normal completion.</p>
 */
public interface LeaderElectionListener {
    /**
     * Called when leadership is acquired by the current process.
     *
     * <p>The leadership period lasts exactly as long as this method's execution. When the method
     * returns (either normally or exceptionally), the leadership is automatically relinquished
     * and new elections begin immediately.
     *
     * <p>For continuous leadership, implementations should:
     * <ul>
     *   <li>Perform all initialization at start</li>
     *   <li>Enter the main leadership loop</li>
     *   <li>Only return when leadership should end</li>
     * </ul>
     *
     * @throws Exception if leadership cannot be maintained or should be terminated early.
     *         The leadership will be released and new elections will begin when any
     *         exception is thrown.
     */
    void takeLeadership() throws Exception;
}
