/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.geogit.repository.Repository;
import org.geotools.util.Range;
import org.springframework.util.Assert;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;

/**
 * Operation to query the commits logs.
 * <p>
 * The list of commits to return can be filtered by setting the following properties:
 * <ul>
 * <li> {@link #setLimit(int) limit}: Limits the number of commits to return.
 * <li> {@link #setTimeRange(Range) timeRange}: return commits that fall in to the given time range.
 * <li> {@link #setSince(ObjectId) since}...{@link #setUntil(ObjectId) until}: Show only commits
 * between the named two commits.
 * <li> {@link #addPath(List) addPath}: Show only commits that affect any of the specified paths.
 * </ul>
 * </p>
 * 
 * @author groldan
 * 
 */
public class LogOp extends AbstractGeoGitOp<Iterator<RevCommit>> {

    private static final Range<Long> ALWAYS = new Range<Long>(Long.class, 0L, true, Long.MAX_VALUE,
            true);

    private Range<Long> timeRange;

    private Integer limit;

    private ObjectId since;

    private ObjectId until;

    private Set<List<String>> paths;

    public LogOp(final Repository repository) {
        super(repository);
        timeRange = ALWAYS;
    }

    public LogOp setLimit(int limit) {
        Assert.isTrue(limit > 0, "limit shall be > 0: " + limit);
        this.limit = Integer.valueOf(limit);
        return this;
    }

    /**
     * Shows only commits
     * 
     * @param the
     *            initial (oldest) commit id, ({@code null} sets the default)
     * @return
     * @see #setUntil(ObjectId)
     */
    public LogOp setSince(final ObjectId since) {
        this.since = since;
        return this;
    }

    /**
     * @param the
     *            final (newest) commit id, ({@code null} sets the default)
     * @return
     * @see #setSince(ObjectId)
     */
    public LogOp setUntil(ObjectId until) {
        this.until = until;
        return this;
    }

    /**
     * @see #addPath(List)
     */
    public LogOp addPath(final String... path) {
        Assert.notNull(path);
        return addPath(Arrays.asList(path));
    }

    /**
     * Show only commits that affect any of the specified paths.
     * 
     * @param path
     * @return
     */
    public LogOp addPath(final List<String> path) {
        Assert.notNull(path);
        if (this.paths == null) {
            this.paths = new HashSet<List<String>>();
        }
        this.paths.add(new ArrayList<String>(path));
        return this;
    }

    public LogOp setTimeRange(final Range<Date> commitRange) {
        if (commitRange == null) {
            this.timeRange = ALWAYS;
        } else {
            this.timeRange = new Range<Long>(Long.class, commitRange.getMinValue().getTime(),
                    commitRange.isMinIncluded(), commitRange.getMaxValue().getTime(),
                    commitRange.isMaxIncluded());
        }
        return this;
    }

    /**
     * @return the list of commits that satisfy the query criteria, most recent first.
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    @Override
    public Iterator<RevCommit> call() throws Exception {
        final Repository repository = getRepository();

        ObjectId newestCommitId;
        ObjectId oldestCommitId;
        {
            if (this.until == null) {
                Ref head = repository.getRef(Ref.HEAD);
                newestCommitId = head.getObjectId();
            } else {
                if (!repository.commitExists(this.until)) {
                    throw new IllegalStateException("Provided 'until' commit id does not exist: "
                            + until.toString());
                }
                newestCommitId = this.until;
            }
            if (this.since == null) {
                oldestCommitId = ObjectId.NULL;
            } else {
                if (!repository.commitExists(this.since)) {
                    throw new IllegalStateException("Provided 'since' commit id does not exist: "
                            + since.toString());
                }
                oldestCommitId = this.since;
            }
        }

        Iterator<RevCommit> linearHistory = new LinearHistoryIterator(newestCommitId, repository);
        LogFilter filter = new LogFilter(repository, oldestCommitId, timeRange, paths);
        Iterator<RevCommit> filteredCommits = Iterators.filter(linearHistory, filter);
        if (limit != null) {
            filteredCommits = Iterators.limit(filteredCommits, limit.intValue());
        }
        return filteredCommits;
    }

    /**
     * Iterator that traverses the commit history backwards starting from the provided commmit
     * 
     * @author groldan
     * 
     */
    private static class LinearHistoryIterator extends AbstractIterator<RevCommit> {

        private ObjectId nextCommitId;

        private final Repository repo;

        public LinearHistoryIterator(final ObjectId tip, final Repository repo) {
            this.nextCommitId = tip;
            this.repo = repo;
        }

        @Override
        protected RevCommit computeNext() {
            if (nextCommitId.isNull()) {
                return endOfData();
            }
            final RevCommit commit = repo.getCommit(nextCommitId);
            List<ObjectId> parentIds = commit.getParentIds();
            Assert.notNull(parentIds);
            Assert.isTrue(parentIds.size() > 0);

            nextCommitId = commit.getParentIds().get(0);

            return commit;
        }

    }

    /**
     * Checks whether the given commit satisfies all the filter criteria set to this op.
     * 
     * @return {@code true} if the commit satisfies the filter criteria set to this op
     */
    private static class LogFilter implements Predicate<RevCommit> {

        private boolean toReached;

        private final ObjectId oldestCommitId;

        private final Range<Long> timeRange;

        private final Set<List<String>> paths;

        private final Repository repo;

        public LogFilter(final Repository repo, final ObjectId oldestCommitId,
                final Range<Long> timeRange, final Set<List<String>> paths) {
            Assert.notNull(repo);
            Assert.notNull(oldestCommitId);
            Assert.notNull(timeRange);

            this.repo = repo;
            this.oldestCommitId = oldestCommitId;
            this.timeRange = timeRange;
            this.paths = paths;
        }

        /**
         * @return {@code true} if the commit satisfies the filter criteria set to this op
         * @see com.google.common.base.Predicate#apply(java.lang.Object)
         */
        @Override
        public boolean apply(final RevCommit commit) {
            if (toReached) {
                return false;
            }
            if (oldestCommitId.equals(commit.getId())) {
                toReached = true;
            }
            boolean applies = timeRange.contains(Long.valueOf(commit.getTimestamp()));
            if (!applies) {
                return false;
            }
            if (paths != null && paths.size() > 0) {
                // did this commit touch any of the paths?
                for (List<String> path : paths) {
                    DiffOp diff = new DiffOp(repo);
                    ObjectId parentId = commit.getParentIds().get(0);
                    Iterator<DiffEntry> diffResult;
                    try {
                        diff.setOldVersion(parentId).setNewVersion(commit.getId()).setFilter(path);
                        diffResult = diff.call();
                        applies = applies && diffResult.hasNext();
                        if (applies) {
                            break;
                        }
                    } catch (Exception e) {
                        Throwables.propagate(e);
                    }
                }
            }

            return applies;
        }
    }
}
